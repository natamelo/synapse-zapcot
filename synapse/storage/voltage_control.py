import logging

from synapse.storage._base import SQLBaseStore
from synapse.api.errors import StoreError
from twisted.internet import defer
from synapse.api.constants import SolicitationStatus

from synapse.api.constants import SolicitationSortParams

logger = logging.getLogger(__name__)


class VoltageControlStore(SQLBaseStore):

    @defer.inlineCallbacks
    def create_solicitation(self, action, equipment, substation, bar, userId, ts, status, value):
        try:
            yield self._simple_insert(
                table="voltage_control_solicitation",
                values={
                    "id": self._voltage_list_id_gen.get_next(),
                    "action_code": action,
                    "equipment_code": equipment,
                    "substation_code": substation,
                    "bar": bar,
                    "request_user_id": userId,
                    "creation_timestamp": ts,
                    "status": status,
                    "value_": value,
                }
            )
        except Exception as e:
            logger.warning("create_solicitation failed: %s",e)
            raise StoreError(500, "Problem creating solicitation.")

    @defer.inlineCallbacks
    def get_substations(self):
        try:
            substations = yield self._simple_select_list(
                "substation",
                keyvalues=None,
                retcols=("code", "name", "company_code"),
            )

            return substations
        except Exception as e:
            logger.warning("get_substation failed: %s", e)
            raise StoreError(500, "Problem recovering substations")

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        try:
            result = yield self._simple_select_one(
                "voltage_control_solicitation",
                {"id": id},
                retcols=("action_code", "equipment_code", "substation_code", "bar",
                 "value_", "request_user_id", "creation_timestamp", "status"),
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
        company_code=None,
        sort_params=None,
        exclude_expired=None,
        table_code=None,
        from_id=0,
        limit=50
    ):

        order_clause  = self._get_order_by_sort_params(sort_params)
        
        expire_clause = ""
        if exclude_expired == "true":
            expire_clause = "and solicitation.status <> 'EXPIRED'"

        def get_solicitations_by_table_code(txn):
            args = [company_code, table_code, from_id, limit]

            sql = (
                " SELECT "
                " solicitation.id,"
                " solicitation.action_code,"
                " solicitation.equipment_code, "
                " solicitation.substation_code, "
                " solicitation.bar, "
                " solicitation.request_user_id, "
                " solicitation.creation_timestamp, "
                " solicitation.status, "
                " solicitation.value_ "
                " from voltage_control_solicitation solicitation, "
                " substation_table table_, substation substation "
                " where table_.substation_code = solicitation.substation_code and "
                " substation.company_code = ? and substation.code = solicitation.substation_code "
                " and table_.table_code = ? and solicitation.id >= ? %s "
                " %s "
                " LIMIT ? "
                %(expire_clause, order_clause)
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
                " solicitation.bar, "
                " solicitation.request_user_id, "
                " solicitation.creation_timestamp, "
                " solicitation.status, "
                " solicitation.value_ "
                " from voltage_control_solicitation solicitation, substation substation "
                " where solicitation.substation_code = substation.code and "
                " substation.company_code = ? and solicitation.id >= ? %s "
                " %s "
                " LIMIT ? "
                %(expire_clause, order_clause)
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
                " solicitation.bar, "
                " solicitation.request_user_id, "
                " solicitation.creation_timestamp, "
                " solicitation.status, "
                " solicitation.value_ "
                " from voltage_control_solicitation solicitation "
                " where solicitation.id >= ? %s "
                " %s "
                " LIMIT ? "
                %(expire_clause, order_clause)
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

    
    def _get_order_by_sort_params(self, sort_params):
        order_clause = ""

        order_by_status = (
            "CASE WHEN solicitation.status = 'NOT_ANSWERED' then '1' " 
                 "WHEN solicitation.status = 'EXPIRED' then '2' "
                 "WHEN solicitation.status = 'AWARE' then '3' "
                 "ELSE solicitation.status END ASC "
        )
        order_by_substation = "solicitation.substation_code ASC"
        order_by_creation = "solicitation.creation_timestamp"

        if (SolicitationSortParams.STATUS in sort_params and
                SolicitationSortParams.SUBSTATION in sort_params and
                SolicitationSortParams.CREATION_TIME in sort_params):
            order_clause = "ORDER BY " + order_by_status + ', ' + order_by_creation + ', ' + order_by_substation                          
        elif (SolicitationSortParams.SUBSTATION in sort_params and
                SolicitationSortParams.CREATION_TIME in sort_params):
            order_clause = "ORDER BY " + order_by_creation + ', ' + order_by_substation
        elif (SolicitationSortParams.STATUS in sort_params and
                SolicitationSortParams.CREATION_TIME in sort_params):
            order_clause = "ORDER BY " + order_by_status + ', ' + order_by_creation
        elif (SolicitationSortParams.STATUS in sort_params and
                 SolicitationSortParams.SUBSTATION in sort_params):
            order_clause = "ORDER BY " + order_by_status + ', ' + order_by_substation
        elif (SolicitationSortParams.SUBSTATION in sort_params):
            order_clause = "ORDER BY solicitation.substation_code ASC"
        elif (SolicitationSortParams.CREATION_TIME in sort_params):
            order_clause = "ORDER BY solicitation.creation_timestamp DESC"

        return order_clause
