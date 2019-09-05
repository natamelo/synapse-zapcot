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
            logger.info("ID TYPE %r %r", type(self._voltage_list_id_gen.get_next()), self._voltage_list_id_gen.get_next())
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
                }
            )
        except Exception as e:
            logger.warning("create_solicitation failed: %s",e)
            raise StoreError(500, "Problem creating solicitation.")