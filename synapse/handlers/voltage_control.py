# -*- coding: utf-8 -*-
# Copyright 2016 OpenMarket Ltd
# Copyright 2018 New Vector Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
from synapse.api.constants import EventTypes

from ._base import BaseHandler
from twisted.internet import defer

from synapse.api.constants import (
    SolicitationStatus,
    SolicitationActions,
    EquipmentTypes,
    VoltageTransformerLevels,
    Companies,
)

from synapse.api.errors import (
    Codes,
    InvalidClientTokenError,
    SynapseError,
)

from synapse.types import UserID

import calendar
import time

from synapse.metrics.background_process_metrics import run_as_background_process

logger = logging.getLogger(__name__)


class VoltageControlHandler(BaseHandler):
    __STATE_MACHINE_ONS = {
        SolicitationStatus.NEW: [SolicitationStatus.CANCELED],
        SolicitationStatus.ACCEPTED: [],
        SolicitationStatus.EXECUTED: [],
        SolicitationStatus.BLOCKED: [SolicitationStatus.CANCELED],
        SolicitationStatus.CONTESTED: [SolicitationStatus.REQUIRED,
                                       SolicitationStatus.CANCELED],
        SolicitationStatus.CANCELED: [],
        SolicitationStatus.REQUIRED: [],
        SolicitationStatus.LATE: []
    }

    __STATE_MACHINE_CTEEP = {
        SolicitationStatus.NEW: [SolicitationStatus.ACCEPTED,
                                 SolicitationStatus.CONTESTED,
                                 SolicitationStatus.BLOCKED,
                                 ],
        SolicitationStatus.ACCEPTED: [SolicitationStatus.BLOCKED,
                                      SolicitationStatus.EXECUTED,
                                      SolicitationStatus.CONTESTED],
        SolicitationStatus.EXECUTED: [],
        SolicitationStatus.BLOCKED: [],
        SolicitationStatus.CONTESTED: [],
        SolicitationStatus.CANCELED: [],
        SolicitationStatus.REQUIRED: [SolicitationStatus.ACCEPTED],
        SolicitationStatus.LATE: [SolicitationStatus.EXECUTED,
                                  SolicitationStatus.BLOCKED]
    }

    __STATE_MACHINE_BOT = {
        SolicitationStatus.NEW: [],
        SolicitationStatus.ACCEPTED: [SolicitationStatus.LATE],
        SolicitationStatus.EXECUTED: [],
        SolicitationStatus.BLOCKED: [],
        SolicitationStatus.CONTESTED: [],
        SolicitationStatus.CANCELED: [],
        SolicitationStatus.REQUIRED: [],
        SolicitationStatus.LATE: []
    }

    def __init__(self, hs):
        self.hs = hs
        self.substation_handler = hs.get_substation_handler()
        self._room_creation_handler = hs.get_room_creation_handler()
        self._room_member_handler = hs.get_room_member_handler()
        self.profiler_handler = hs.get_profile_handler()
        self.store = hs.get_datastore()
        self.notifier = hs.get_notifier()

    @defer.inlineCallbacks
    def create_solicitations(self, requester, solicitations, creation_total_time):
        check_param_create_total_time(creation_total_time)
        group_id = yield self.store.create_solicitation_group(creation_total_time)

        user_id = requester.user.to_string()
        for solicitation in solicitations:
            treat_solicitation_data(solicitation)
            yield self.check_substation(solicitation['company_code'], solicitation['substation'])
            yield check_solicitation_params(solicitation)

        # Enquanto não tem as permissões, recupera todos os usuários.
        users = yield self.store.get_users()

        for solicitation in solicitations:
            solicitation_created = yield self._create_solicitation(solicitation, user_id, group_id, None)

            token = yield self.store.create_solicitation_updated_event(EventTypes.CreateSolicitation,
                                                                       solicitation_created['id'],
                                                                       user_id, solicitation)

            self.notifier.on_new_event("solicitations_key", token, [user["name"] for user in users])

            self.create_room_and_join_users(requester, users, solicitation_created['id'],
                                            solicitation_created['substation_code'],
                                            solicitation_created['equipment_code'])

    @defer.inlineCallbacks
    def _create_solicitation(self, solicitation, user_id, group_id, room_id):
        ts = calendar.timegm(time.gmtime())

        solicitation_id = yield self.store.create_solicitation(
            action=solicitation["action"],
            equipment=solicitation["equipment"],
            substation=solicitation["substation"],
            staggered=solicitation["staggered"],
            amount=solicitation["amount"],
            voltage=solicitation["voltage"],
            at=solicitation["at"],
            bt=solicitation["bt"],
            user_id=user_id,
            ts=ts,
            status=SolicitationStatus.NEW,
            group_id=group_id,
            room_id=room_id)

        solicitation = yield self.get_solicitation_by_id(solicitation_id)

        return solicitation

    @defer.inlineCallbacks
    def create_room_for_solicitation(self, requester, users, substation, equipment):
        room_name = "%s - %s" % (equipment, substation)
        room_id = yield self._room_creation_handler.create_room_for_solicitation(requester, room_name, users)
        return room_id

    def get_timestamp_by_solicitation_status(self, solicitation, status):
        for event in solicitation["events"]:
            if event['status'] == status:
                return event["time_stamp"]
        return 0

    @defer.inlineCallbacks
    def add_creators_to_solicitations(self, solicitations):
        creation_index = -1
        for solicitation in solicitations:
            creator_id = solicitation['events'][creation_index]['user_id']
            if creator_id:
                creator_profile = yield self.profiler_handler.get_profile(creator_id)
                solicitation['created_by'] = creator_profile

    @defer.inlineCallbacks
    def get_solicitations(self, is_order_by_cteep, from_id=0, limit=1000):
        result = yield self.store.get_solicitations(
            is_order_by_cteep=is_order_by_cteep,
            from_id=from_id,
            limit=limit
        )
        yield self.add_creators_to_solicitations(result)
        return result

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        solicitation = yield self.store.get_solicitation_by_id(id=id)
        return solicitation

    @defer.inlineCallbacks
    def change_solicitation_status(self, new_status, id, user_id):
        solicitation = yield self.get_solicitation_by_id(id)

        if not solicitation:
            raise SynapseError(404, "Solicitation not found.", Codes.NOT_FOUND)

        if new_status not in SolicitationStatus.ALL_SOLICITATION_TYPES:
            raise SynapseError(400, "Invalid status.", Codes.INVALID_PARAM)

        last_event_index = 0
        first_event_index = -1
        current_status = solicitation["events"][last_event_index]["status"]
        creation_ts = solicitation["events"][first_event_index]["time_stamp"]

        user_company_code = yield self.store.get_company_code(user_id)

        self._validate_status_change(current_status, new_status, user_company_code, creation_ts)

        ts = calendar.timegm(time.gmtime())
        yield self.store.create_solicitation_status_signature(id, user_id, new_status, ts)

        token = yield self.store.create_solicitation_updated_event(
            EventTypes.ChangeSolicitationStatus, id, user_id, {"status": new_status}
        )

        # Enquanto não tem as permissões, recupera todos os usuários.
        users = yield self.store.get_users()

        self.notifier.on_new_event("solicitations_key", token, [user["name"] for user in users])

    def start_updating_late_solicitations(self):
        run_as_background_process(
            "sync_late_solicitations", self._update_status_from_late_solicitations
        )

    @defer.inlineCallbacks
    def _update_status_from_late_solicitations(self):
        current_time = calendar.timegm(time.gmtime())
        solicitations = yield self.store.get_late_solicitations_with_status_new(current_time)
        for solicitation in solicitations:
            # TODO Put the bot user_id correct in the future
            yield self.change_solicitation_status(SolicitationStatus.LATE, solicitation['id'], None)

    @defer.inlineCallbacks
    def _join_users_to_room(self, requester, users, room_id):
        for user in users:
            user_object = UserID.from_string(user)
            yield self._room_member_handler.update_membership(requester, user_object, room_id, 'join')

    @defer.inlineCallbacks
    def _create_room_and_join_users(self, requester, users, solicitation_id, substation, equipment):
        requester_user_id = requester.user.to_string()
        users_to_invite = self._get_users_name_to_invite(users, requester_user_id)

        room_id = yield self.create_room_for_solicitation(requester, users_to_invite, substation, equipment)

        yield self.store.update_solicitation_room(solicitation_id, room_id)

        yield self._join_users_to_room(requester, users_to_invite, room_id)

    def create_room_and_join_users(self, requester, users, solicitation_id, substation, equipment):
        run_as_background_process(
            "create_room_and_join_users", self._create_room_and_join_users, requester, users,
            solicitation_id, substation, equipment
        )

    @classmethod
    def _get_users_name_to_invite(cls, users, user_to_remove):
        users_to_invite = []
        for user in users:
            if user['name'] != user_to_remove:
                users_to_invite.append(user['name'])
        return users_to_invite

    @classmethod
    def _get_state_machine_by_user_company(cls, company_code):
        if company_code == Companies.ONS:
            return VoltageControlHandler.__STATE_MACHINE_ONS
        elif company_code == Companies.CTEEP:
            return VoltageControlHandler.__STATE_MACHINE_CTEEP

        return VoltageControlHandler.__STATE_MACHINE_BOT

    @classmethod
    def _validate_status_change(cls, current_status, new_status, user_company_code, creation_ts):

        if current_status == new_status:
            raise SynapseError(400, "Status has already changed.", Codes.INVALID_PARAM)

        state_machine = cls._get_state_machine_by_user_company(user_company_code)
        next_states_possible = state_machine[current_status]

        if new_status not in next_states_possible:
            raise SynapseError(400, "Inconsistent status change.", Codes.INVALID_PARAM)

        # if new_status == SolicitationStatus.ACCEPTED:
        #    cls._check_timeout_to_accept(creation_ts)

    @classmethod
    def _check_timeout_to_accept(cls, creation_ts):
        result = calendar.timegm(time.gmtime()) - int(creation_ts)
        if result > 300:  # 300 = 5 minutes in timestamp
            raise SynapseError(400, "Expired solicitation!", Codes.INVALID_PARAM)

    @defer.inlineCallbacks
    def check_substation(self, company_code, substation):
        substation_object = yield self.substation_handler. \
            get_substation_by_company_code_and_substation_code(company_code, substation)
        if substation_object is None:
            raise SynapseError(400, "Invalid substation!", Codes.INVALID_PARAM)


def treat_solicitation_data(solicitation):
    if "voltage" not in solicitation:
        solicitation["voltage"] = None

    if solicitation["equipment"] == EquipmentTypes.SYNCHRONOUS:
        solicitation["staggered"] = None

        if solicitation["action"] != SolicitationActions.ADJUST:
            solicitation["amount"] = None

    if solicitation["equipment"] == EquipmentTypes.TRANSFORMER:
        solicitation["staggered"] = None

        if "at" not in solicitation:
            solicitation["at"] = None

        if "bt" not in solicitation:
            solicitation["bt"] = None

    else:
        solicitation["at"] = None
        solicitation["bt"] = None


def check_param_create_total_time(create_total_time):
    try:
        int(create_total_time)
    except ValueError:
        raise SynapseError(400, "Invalid creation total time!", Codes.INVALID_PARAM)


def check_solicitation_params(solicitation):
    if solicitation["action"] not in SolicitationActions.ALL_ACTIONS:
        raise SynapseError(400, "Invalid action!", Codes.INVALID_PARAM)
    if solicitation["equipment"] not in EquipmentTypes.ALL_EQUIPMENT:
        raise SynapseError(400, "Invalid Equipment!", Codes.INVALID_PARAM)

    if solicitation["equipment"] == EquipmentTypes.REACTOR:
        check_reactor_params(solicitation)
    elif solicitation["equipment"] == EquipmentTypes.CAPACITOR:
        check_capacitor_params(solicitation)
    elif solicitation["equipment"] == EquipmentTypes.TRANSFORMER:
        check_transform_params(solicitation)
    elif solicitation["equipment"] == EquipmentTypes.SYNCHRONOUS:
        check_synchronous_params(solicitation)


def check_reactor_params(solicitation):
    check_action_type(
        action=solicitation["action"],
        possible_actions=[SolicitationActions.TURN_ON, SolicitationActions.TURN_OFF],
        equipment_type=solicitation["equipment"]
    )

    check_amount(
        amount=solicitation["amount"],
        min_value=1,
        equipment_type=solicitation["equipment"]
    )

    check_staggered(
        staggered=solicitation["staggered"],
        equipment_type=solicitation["equipment"]
    )

    check_voltage(
        voltage=solicitation["voltage"],
        equipment_type=solicitation["equipment"]
    )


def check_capacitor_params(solicitation):
    check_action_type(
        action=solicitation["action"],
        possible_actions=[SolicitationActions.TURN_ON, SolicitationActions.TURN_OFF],
        equipment_type=solicitation["equipment"]
    )

    check_amount(
        amount=solicitation["amount"],
        min_value=1,
        equipment_type=solicitation["equipment"]
    )

    check_staggered(
        staggered=solicitation["staggered"],
        equipment_type=solicitation["equipment"]
    )

    check_voltage(
        voltage=solicitation["voltage"],
        equipment_type=solicitation["equipment"]
    )


def check_transform_params(solicitation):
    check_action_type(
        action=solicitation["action"],
        possible_actions=[SolicitationActions.RISE, SolicitationActions.REDUCE,
                          SolicitationActions.ADJUST_FOR_TAPE, SolicitationActions.ADJUST],
        equipment_type=solicitation["equipment"]
    )

    check_voltage(
        voltage=solicitation["voltage"],
        equipment_type=solicitation["equipment"]
    )

    if solicitation["action"] == SolicitationActions.ADJUST_FOR_TAPE:
        check_amount(
            amount=solicitation["amount"],
            min_value=-100000,
            equipment_type=solicitation["equipment"]
        )

    if solicitation["action"] == SolicitationActions.RISE or \
            solicitation["action"] == SolicitationActions.REDUCE or \
            solicitation["action"] == SolicitationActions.ADJUST:
        check_amount(
            amount=solicitation["amount"],
            min_value=1,
            equipment_type=solicitation["equipment"]
        )

    check_at(
        voltage=solicitation["at"],
        equipment_type=solicitation["equipment"]
    )

    check_bt(
        voltage=solicitation["bt"],
        equipment_type=solicitation["equipment"]
    )


def check_synchronous_params(solicitation):
    check_action_type(
        action=solicitation["action"],
        possible_actions=[SolicitationActions.MAXIMIZE, SolicitationActions.RESET,
                          SolicitationActions.MINIMIZE, SolicitationActions.ADJUST],
        equipment_type=solicitation["equipment"]
    )

    if solicitation["action"] == SolicitationActions.ADJUST:
        check_amount(
            amount=solicitation["amount"],
            min_value=1,
            equipment_type=solicitation["equipment"])


def check_action_type(action, possible_actions, equipment_type):
    if action not in possible_actions:
        raise SynapseError(
            400,
            "Invalid action for equipment type '%s'." % (equipment_type),
            Codes.INVALID_PARAM
        )


def check_amount(amount, min_value, equipment_type):
    try:
        if int(amount) < min_value:
            raise SynapseError(
                400,
                "Invalid amount value for equipment type '%s'." % equipment_type,
                Codes.INVALID_PARAM
            )
    except Exception as e:
        raise SynapseError(
            400,
            "Invalid amount value for equipment type '%s'." % equipment_type,
            Codes.INVALID_PARAM
        )


def check_staggered(staggered, equipment_type):
    if type(staggered) != bool:
        raise SynapseError(
            400,
            "Invalid staggered for equipment type '%s'." % equipment_type,
            Codes.INVALID_PARAM
        )


def check_voltage(voltage, equipment_type):
    is_optional = equipment_type == EquipmentTypes.REACTOR or \
                  equipment_type == EquipmentTypes.CAPACITOR

    if is_optional and voltage is None:
        return

    if voltage not in VoltageTransformerLevels.ALL_ALLOWED_LEVELS:
        raise SynapseError(
            400,
            "Invalid voltage value for equipment type '%s'." % equipment_type,
            Codes.INVALID_PARAM
        )


def check_at(voltage, equipment_type):
    if voltage not in VoltageTransformerLevels.ALL_ALLOWED_LEVELS:
        raise SynapseError(
            400,
            "Invalid AT value for equipment type '%s'." % equipment_type,
            Codes.INVALID_PARAM
        )


def check_bt(voltage, equipment_type):
    if voltage not in VoltageTransformerLevels.ALL_ALLOWED_LEVELS:
        raise SynapseError(
            400,
            "Invalid BT value for equipment type '%s'." % equipment_type,
            Codes.INVALID_PARAM
        )
