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

import calendar
import time

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

    def __init__(self, hs):
        self.hs = hs
        self.substation_handler = hs.get_substation_handler()
        self.profiler_handler = hs.get_profile_handler()
        self.store = hs.get_datastore()

    @defer.inlineCallbacks
    def create_solicitations(self, solicitations, user_id):
        status = SolicitationStatus.NEW

        for solicitation in solicitations:
            treat_solicitation_data(solicitation)
            yield self.check_substation(solicitation['company_code'], solicitation['substation'])
            yield check_solicitation_params(solicitation)

        for solicitation in solicitations:
            ts = calendar.timegm(time.gmtime())

            yield self.store.create_solicitation(
                action=solicitation["action"],
                equipment=solicitation["equipment"],
                substation=solicitation["substation"],
                staggered=solicitation["staggered"],
                amount=solicitation["amount"],
                voltage=solicitation["voltage"],
                user_id=user_id,
                ts=ts,
                status=status
            )

    @defer.inlineCallbacks
    def add_creators_to_solicitations(self, solicitations):
        for solicitation in solicitations:
            creator_id = solicitation['events'][0]['user_id']
            creator_profile = yield self.profiler_handler.get_profile(creator_id)
            solicitation['created_by'] = creator_profile

    @defer.inlineCallbacks
    def filter_solicitations(self, company_code, substations, sort_params, exclude_expired, table_code, from_id, limit):
        result = yield self.store.get_solicitations_by_params(
            company_code=company_code,
            substations=substations,
            table_code=table_code,
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
        yield self.store.create_solicitation_event(id, user_id, new_status, ts)

    @classmethod
    def _get_state_machine_by_user_company(cls, company_code):
        if company_code == Companies.ONS:
            return VoltageControlHandler.__STATE_MACHINE_ONS

        return VoltageControlHandler.__STATE_MACHINE_CTEEP

    @classmethod
    def _validate_status_change(cls, current_status, new_status, user_company_code, creation_ts):

        if current_status == new_status:
            raise SynapseError(400, "Status has already changed.", Codes.INVALID_PARAM)

        state_machine = cls._get_state_machine_by_user_company(user_company_code)
        next_states_possible = state_machine[current_status]

        if new_status not in next_states_possible:
            raise SynapseError(400, "Inconsistent status change.", Codes.INVALID_PARAM)

        if new_status == SolicitationStatus.ACCEPTED:
            cls._check_timeout_to_accept(creation_ts)

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
