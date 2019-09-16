import logging

from synapse.storage._base import SQLBaseStore
from synapse.api.errors import StoreError
from twisted.internet import defer

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
    def get_solicitations_by_params(self, company_code, from_id=0, limit=50):
        def get_solicitations(txn):
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
                " substation.company_code = ? and solicitation.id >= ? "
                " ORDER BY solicitation.creation_timestamp DESC"
                " LIMIT ? "
            )
            txn.execute(sql, args)

            return self.cursor_to_dict(txn)

        results = yield self.runInteraction(
            "get_solicitations_by_params", get_solicitations
        )

        defer.returnValue(results)
