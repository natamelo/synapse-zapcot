import logging

from synapse.storage._base import SQLBaseStore
from synapse.api.errors import StoreError
from twisted.internet import defer

import collections

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
            subs = yield self._simple_select_list(
                "substation",
                keyvalues=None,
                retcols=("code", "name", "company_code"),
            )

            return subs
        except Exception as e:
            logger.warning("get_substation failed: %s", e)
            raise StoreError(500, "Problem recovering substations")

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        try:
            result = yield self._simple_select_one(
                "voltage_control_solicitation",
                dict(id=id),
                retcols=("action_code", "equipment_code", "substation_code", "bar",
                 "value_", "request_user_id", "creation_timestamp", "status"),
                allow_none=True,
            )
            if result:
                return result
            return None
        except Exception as e:
            logger.warning("ged_solicitation failed: %s", e)
            raise StoreError(500, "Problem recovering solicitation")

    @defer.inlineCallbacks
    def change_solicitation_status(self, new_status, id):
        try:
            yield self._simple_update_one(
                table="voltage_control_solicitation",
                keyvalues={"id": id},
                updatevalues={"status": new_status},
            )
        except Exception as e:
            logger.warning("change_solicitation_status failed: %s", e)
            raise StoreError(500, "Problem on update solicitation")
