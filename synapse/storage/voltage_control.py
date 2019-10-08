import logging

from synapse.storage._base import SQLBaseStore
from synapse.api.errors import StoreError
from twisted.internet import defer
from synapse.api.constants import SolicitationStatus

from synapse.api.constants import SolicitationSortParams

logger = logging.getLogger(__name__)


class VoltageControlStore(SQLBaseStore):

    @defer.inlineCallbacks
    def create_solicitation(self, action, equipment, substation, staggered, amount, voltage, user_id, ts, status):
        try:
            next_id = self._voltage_list_id_gen.get_next()
            yield self._simple_insert(
                table="voltage_control_solicitation",
                values={
                    "id": next_id,
                    "action_code": action,
                    "equipment_code": equipment,
                    "substation_code": substation,
                    "staggered": staggered,
                    "amount": amount,
                    "voltage": voltage
                }
            )

            yield self._simple_insert(
                table="solicitation_event",
                values={
                    "user_id": user_id,
                    "status": status,
                    "time_stamp": ts,
                    "voltage_control_id": next_id,
                }
            )
        except Exception as e:
            logger.warning("create_solicitation failed: %s", e)
            raise StoreError(500, "Problem creating solicitation.")

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        try:
            result = yield self._simple_select_one(
                "voltage_control_solicitation",
                {"id": id},
                retcols=("action_code", "equipment_code", "substation_code", "amount",
                         "voltage"),
                allow_none=True,
            )
            if result:
                return result
            return None
        except Exception as e:
            logger.warning("get_solicitation failed: %s", e)
            raise StoreError(500, "Problem recovering solicitation")

    @defer.inlineCallbacks
    def change_solicitation_status(self, new_status, solicitation_id, user_id, ts):
        try:

            self.get_solicitation_by_id(solicitation_id)

            yield self._simple_insert(
                table="solicitation_event",
                values={
                    "user_id": user_id,
                    "status": new_status,
                    "time_stamp": ts,
                    "voltage_control_id": solicitation_id,
                }
            )

        except Exception as e:
            logger.warning("change_solicitation_status failed: %s", e)
            raise StoreError(500, "Problem on update solicitation")

    def get_events_by_solicitation_id(self, solicitation_id):
        return self._simple_select_list(
            table="solicitation_event",
            keyvalues={"voltage_control_id": solicitation_id},
            retcols=("user_id", "status", "time_stamp"),
            desc="get_users_in_group",
        )

    @defer.inlineCallbacks
    def get_solicitations_by_params(
        self,
        substations=None,
        company_code=None,
        sort_params=None,
        exclude_expired=None,
        table_code=None,
        from_id=0,
        limit=50
    ):

        #Refazer ordenação e filtro baseado nos eventos de solicitação
        #order_clause = get_order_clause_by_sort_params(sort_params)
        filter_clause = get_filter_clause(substations, exclude_expired)

        def get_solicitations_by_table_code(txn):
            args = [company_code, table_code, from_id, limit]

            sql = (
                " SELECT "
                " solicitation.id,"
                " solicitation.action_code,"
                " solicitation.equipment_code, "
                " solicitation.substation_code, "
                " solicitation.amount, "
                " solicitation.voltage "
                " from voltage_control_solicitation solicitation, "
                " substation_table table_, substation substation "
                " where table_.substation_code = solicitation.substation_code and "
                " substation.company_code = ? and substation.code = solicitation.substation_code "
                " and table_.table_code = ? and solicitation.id >= ? %s "
                " LIMIT ? "
                % filter_clause
            )
            txn.execute(sql, args)

            return self.cursor_to_dict(txn)

        def get_solicitations_by_company_code(txn):
            args = [company_code, from_id, limit]

            sql = (
                " SELECT "
                " solicitation.id,"
                " solicitation.action_code,"
                " solicitation.equipment_code, "
                " solicitation.substation_code, "
                " solicitation.amount, "
                " solicitation.voltage "
                " from voltage_control_solicitation solicitation, substation substation "
                " where solicitation.substation_code = substation.code and "
                " substation.company_code = ? and solicitation.id >= ? %s "
                " LIMIT ? "
                % filter_clause
            )
            txn.execute(sql, args)

            return self.cursor_to_dict(txn)

        def get_all_solicitations(txn):
            args = [from_id, limit]

            sql = (
                " SELECT "
                " solicitation.id,"
                " solicitation.action_code,"
                " solicitation.equipment_code, "
                " solicitation.substation_code, "
                " solicitation.amount, "
                " solicitation.voltage "
                " from voltage_control_solicitation solicitation "
                " where solicitation.id >= ? %s "
                " LIMIT ? "
                % filter_clause
            )
            txn.execute(sql, args)

            return self.cursor_to_dict(txn)

        if table_code and company_code:
            query_to_call = get_solicitations_by_table_code
        elif company_code:
            query_to_call = get_solicitations_by_company_code
        else:
            query_to_call = get_all_solicitations

        results = yield self.runInteraction(
            "get_solicitations_by_params", query_to_call
        )

        for solicitation in results:
            events = yield self.get_events_by_solicitation_id(solicitation['id'])
            solicitation['events'] = events

        defer.returnValue(results)


def get_filter_clause(substations, exclude_expired):

    filter_clause = ""

    #if exclude_expired == "true":
    #    filter_clause = "and solicitation.status <> 'EXPIRED' "

    if substations:
        if len(substations) > 1:
            filter_clause = filter_clause + "and solicitation.substation_code IN " + str(tuple(substations))
        elif len(substations) == 1:
            filter_clause = filter_clause + "and solicitation.substation_code = '" + substations[0] + "'"

    return filter_clause


def get_order_clause_by_sort_params(sort_params):

    order_by_status = (
        "CASE WHEN solicitation.status = 'NOT_ANSWERED' then '1' "
        "WHEN solicitation.status = 'EXPIRED' then '2' "
        "WHEN solicitation.status = 'AWARE' then '3' "
        "ELSE solicitation.status END ASC "
    )

    order_clause = "ORDER BY "

    if not sort_params or SolicitationSortParams.STATUS in sort_params:
        order_clause += order_by_status + ", "
    if sort_params and SolicitationSortParams.CREATION_TIME in sort_params:
        order_clause += "solicitation.creation_timestamp DESC, "
    if sort_params and SolicitationSortParams.SUBSTATION in sort_params:
        order_clause += "solicitation.substation_code ASC, "

    order_clause = order_clause[0:-2]

    return order_clause

