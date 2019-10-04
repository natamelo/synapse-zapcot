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
            yield self._simple_insert(
                table="voltage_control_solicitation",
                values={
                    "id": self._voltage_list_id_gen.get_next(),
                    "action_code": action,
                    "equipment_code": equipment,
                    "substation_code": substation,
                    "staggered": staggered,
                    "amount": amount,
                    "voltage": voltage,
                    "request_user_id": user_id,
                    "creation_timestamp": ts,
                    "status": status
                }
            )
        except Exception as e:
            logger.warning("create_solicitation failed: %s",e)
            raise StoreError(500, "Problem creating solicitation.")

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        try:
            result = yield self._simple_select_one(
                "voltage_control_solicitation",
                {"id": id},
                retcols=("action_code", "equipment_code", "substation_code", "amount",
                 "voltage", "request_user_id", "creation_timestamp", "status"),
                allow_none=True,
            )
            if result:
                return result
            return None
        except Exception as e:
            logger.warning("get_solicitation failed: %s", e)
            raise StoreError(500, "Problem recovering solicitation")

    @defer.inlineCallbacks
    def change_solicitation_status(self, new_status, id, user_id, update_ts):
        try:
            updates = {
                "status":new_status,
                "update_timestamp":update_ts
            }
            if new_status == SolicitationStatus.AWARE:
                updates["response_user_id"] = user_id
            yield self._simple_update_one(
                table="voltage_control_solicitation",
                keyvalues= {"id":id},
                updatevalues=updates,
            )
        except Exception as e:
            logger.warning("change_solicitation_status failed: %s", e)
            raise StoreError(500, "Problem on update solicitation")

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

        order_clause = get_order_clause_by_sort_params(sort_params)
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
                " solicitation.request_user_id, "
                " solicitation.creation_timestamp, "
                " solicitation.status, "
                " solicitation.voltage "
                " from voltage_control_solicitation solicitation, "
                " substation_table table_, substation substation "
                " where table_.substation_code = solicitation.substation_code and "
                " substation.company_code = ? and substation.code = solicitation.substation_code "
                " and table_.table_code = ? and solicitation.id >= ? %s "
                " %s "
                " LIMIT ? "
                % (filter_clause, order_clause)
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
                " solicitation.request_user_id, "
                " solicitation.creation_timestamp, "
                " solicitation.status, "
                " solicitation.voltage "
                " from voltage_control_solicitation solicitation, substation substation "
                " where solicitation.substation_code = substation.code and "
                " substation.company_code = ? and solicitation.id >= ? %s "
                " %s "
                " LIMIT ? "
                % (filter_clause, order_clause)
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
                " solicitation.request_user_id, "
                " solicitation.creation_timestamp, "
                " solicitation.status, "
                " solicitation.voltage "
                " from voltage_control_solicitation solicitation "
                " where solicitation.id >= ? %s "
                " %s "
                " LIMIT ? "
                % (filter_clause, order_clause)
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

        defer.returnValue(results)


def get_filter_clause(substations, exclude_expired):

    filter_clause = ""

    if exclude_expired == "true":
        filter_clause = "and solicitation.status <> 'EXPIRED' "

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

