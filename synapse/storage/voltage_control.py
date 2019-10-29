import logging

from synapse.storage._base import SQLBaseStore
from synapse.api.errors import StoreError
from twisted.internet import defer

from synapse.api.constants import SolicitationSortParams, \
    SolicitationStatus

from canonicaljson import json

logger = logging.getLogger(__name__)


class VoltageControlStore(SQLBaseStore):

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        try:
            result = yield self._simple_select_one(
                "voltage_control_solicitation",
                {"id": id},
                retcols=("id", "action_code", "equipment_code", "substation_code", "amount",
                         "voltage"),
                allow_none=True,
            )
            if result:
                result['events'] = yield self.get_events_by_solicitation_id(id)
                return result
            return None
        except Exception as e:
            logger.warning("get_solicitation failed: %s", e)
            raise StoreError(500, "Problem recovering solicitation")

    @defer.inlineCallbacks
    def create_solicitation_status_signature(self, solicitation_id, user_id, new_status, ts):
        try:

            self.get_solicitation_by_id(solicitation_id)

            signature_id = self._solicitation_signature_id_gen.get_next()

            yield self._simple_insert(
                table="solicitation_status_signature",
                values={
                    "id": signature_id,
                    "user_id": user_id,
                    "status": new_status,
                    "time_stamp": ts,
                    "solicitation_id": solicitation_id,
                }
            )

        except Exception as e:
            logger.warning("change_solicitation_status failed: %s", e)
            raise StoreError(500, "Problem on update solicitation")

    @defer.inlineCallbacks
    def create_solicitation(self, action, equipment, substation, staggered, amount, voltage, user_id, ts, status):
        try:
            solicitation_id = self._solicitation_list_id_gen.get_next()
            yield self._simple_insert(
                table="voltage_control_solicitation",
                values={
                    "id": solicitation_id,
                    "action_code": action,
                    "equipment_code": equipment,
                    "substation_code": substation,
                    "staggered": staggered,
                    "amount": amount,
                    "voltage": voltage
                }
            )

            yield self.create_solicitation_status_signature(solicitation_id, user_id, status, ts)

            return solicitation_id

        except Exception as e:
            logger.warning("create_solicitation failed: %s", e)
            raise StoreError(500, "Problem creating solicitation.")

    @defer.inlineCallbacks
    def create_solicitation_updated_event(self, event_type, solicitation_id, user_id, content):
        try:
            with self._solicitation_updates_id_gen.get_next() as stream_id:
                yield self._simple_insert(
                    table="solicitation_updates",
                    values={
                        "stream_id": stream_id,
                        "solicitation_id": solicitation_id,
                        "user_id": user_id,
                        "type": event_type,
                        "content": json.dumps(content)
                    }
                )
                self._solicitation_updates_stream_cache.entity_has_changed(user_id, stream_id)
                return stream_id

        except Exception as e:
            logger.warning("create_solicitation_updated_event failed: %s", e)
            raise StoreError(500, "Problem creating solicitation update event.")

    @defer.inlineCallbacks
    def associate_solicitation_to_room(self, solicitation_id, room_id):
        try:
            yield self._simple_insert(
                table="solicitation_room",
                values={
                    "solicitation_id": solicitation_id,
                    "room_id": room_id,
                }
            )

        except Exception as e:
            logger.warning("associate solicitation to room failed: %s", e)
            raise StoreError(500, "Problem associating solicitation.")

    @defer.inlineCallbacks
    def get_events_by_solicitation_id(self, solicitation_id):

        def get_events_by_solicitation_id_(txn):
            args = [solicitation_id]

            sql = (
                " SELECT event.user_id, event.status, event.time_stamp "
                " FROM solicitation_status_signature event "
                " WHERE event.solicitation_id = ? "
                " ORDER BY event.time_stamp DESC "
            )
            txn.execute(sql, args)

            return self.cursor_to_dict(txn)

        results = yield self.runInteraction(
            "get_events_by_solicitation_id", get_events_by_solicitation_id_
        )
        return defer.returnValue(results)

    def get_all_solicitation_updates(self, user_id, from_token, to_token):

        from_token = int(from_token)
        has_changed = self._solicitation_updates_stream_cache.has_entity_changed(
            user_id, from_token
        )
        if not has_changed:
            return []

        def _get_all_solicitation_updates_txn(txn):
            sql = """
                SELECT solicitation_id, type, content
                FROM solicitation_updates
                WHERE user_id = ? AND ? < stream_id AND stream_id <= ?
            """
            txn.execute(sql, (user_id, from_token, to_token))
            return [
                {
                    "solicitation_id": solicitation_id,
                    "type": stype,
                    "content": json.loads(content),
                }
                for solicitation_id, stype, content in txn
            ]

        return self.runInteraction(
            "get_all_solicitation_updates", _get_all_solicitation_updates_txn
        )

    def get_solicitation_stream_token(self):
        return self._solicitation_updates_id_gen.get_current_token()

    @defer.inlineCallbacks
    def get_solicitations_by_params(
        self,
        substations=[],
        companies=[],
        tables=[],
        status=[],
        from_id=0,
        limit=1000
    ):

        """ Order By Status and Timestamp
            Group 1: 'BLOCKED', 'CONTESTED', 'NEW', 'REQUIRED' (Timestamp ASC)
            Group 2: 'LATE', 'ACCEPTED' (Timestamp ASC)
            Group 3: 'EXECUTED', 'CANCELED' (Timestamp DESC)
        """

        def get_solicitations(txn):

            args = [from_id, limit]

            sql = (
                " SELECT sol.id, sol.action_code, sol.equipment_code, "
                "        sol.substation_code, sol.staggered, sol.amount, sol.voltage "
                " FROM voltage_control_solicitation sol, solicitation_status_signature sig "
                " WHERE sol.id >= ? AND sig.id = "
                "       (SELECT id "
                "        FROM (SELECT id, MAX(time_stamp) "
                "              FROM solicitation_status_signature "
                "              WHERE sol.id = solicitation_id) "
                "       ) "
                " ORDER BY "
                "   CASE WHEN sig.status IN ('BLOCKED', 'CONTESTED', 'NEW', 'REQUIRED') THEN '1' "
                "        WHEN sig.status IN ('LATE', 'ACCEPTED') THEN '2' "
                "        WHEN sig.status IN ('EXECUTED', 'CANCELED') THEN '3' "
                "   END ASC, "
                "   CASE WHEN sig.status IN ('EXECUTED', 'CANCELED') THEN (sig.time_stamp * -1) "
                "        ELSE sig.time_stamp  "
                "   END ASC "
                "   LIMIT ? "
            )

            txn.execute(sql, args)

            return self.cursor_to_dict(txn)

        query_to_call = get_solicitations

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
    #    filter_clause = "and solicitation.status <> 'LATE' "

    if substations:
        if len(substations) > 1:
            filter_clause = filter_clause + "and solicitation.substation_code IN " + str(tuple(substations))
        elif len(substations) == 1:
            filter_clause = filter_clause + "and solicitation.substation_code = '" + substations[0] + "'"

    return filter_clause


def get_order_clause_by_sort_params(sort_params):

    order_by_status = (
        "CASE WHEN solicitation.status = 'NEW' then '1' "
        "WHEN solicitation.status = 'LATE' then '2' "
        "WHEN solicitation.status = 'ACCEPTED' then '3' "
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

