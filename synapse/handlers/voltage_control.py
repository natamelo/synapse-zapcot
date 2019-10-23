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
    VoltageTransformerLevels
)

from synapse.api.errors import (
    Codes,
    SynapseError,
)

import calendar
import time

logger = logging.getLogger(__name__)


class VoltageControlHandler(BaseHandler):

    def __init__(self, hs):
        self.hs = hs
        self.substation_handler = hs.get_substation_handler()
        self._room_creation_handler = hs.get_room_creation_handler()
        self.store = hs.get_datastore()
        self.notifier = hs.get_notifier()

    @defer.inlineCallbacks
    def create_solicitations(self, requester, solicitations):
        user_id = requester.user.to_string()
        for solicitation in solicitations:
            treat_solicitation_data(solicitation)
            yield self.check_substation(solicitation['company_code'], solicitation['substation'])
            yield check_solicitation_params(solicitation)

        for solicitation in solicitations:
            solicitation_created = yield self.create_solicitation(solicitation, user_id)

            #TODO Liberar a criação de sala depois
            #yield self.create_room_associated_to_solicitation(requester, solicitation_created)
            token = yield self.store.create_solicitation_updated_event(EventTypes.CreateSolicitation,
                                                                       solicitation_created['id'],
                                                                       user_id, solicitation)
            self.notifier.on_new_event("solicitations_key", token)

    @defer.inlineCallbacks
    def create_solicitation(self, solicitation, user_id):
        ts = calendar.timegm(time.gmtime())

        solicitation_id = yield self.store.create_solicitation(
            action=solicitation["action"],
            equipment=solicitation["equipment"],
            substation=solicitation["substation"],
            staggered=solicitation["staggered"],
            amount=solicitation["amount"],
            voltage=solicitation["voltage"],
            user_id=user_id,
            ts=ts,
            status=SolicitationStatus.NEW)

        solicitation = yield self.get_solicitation_by_id(solicitation_id)

        return solicitation

    @defer.inlineCallbacks
    def create_room_associated_to_solicitation(self, requester, solicitation):
        timestamp = self.get_timestamp_by_solicitation_status(solicitation, SolicitationStatus.NEW)
        room_name = "%s - %s (%s)" % (solicitation["equipment_code"], solicitation["substation_code"], timestamp)
        room_id = yield self._room_creation_handler.create_room_by_name(requester, room_name)
        yield self.store.associate_solicitation_to_room(solicitation["id"], room_id)

    def get_timestamp_by_solicitation_status(self, solicitation, status):
        for event in solicitation["events"]:
            if event['status'] == status:
                return event["time_stamp"]
        return 0

    @defer.inlineCallbacks
    def filter_solicitations(self, company_code, substations, sort_params, exclude_expired, table_code, from_id, limit):
        result = yield self.store.get_solicitations_by_params(
            company_code=company_code,
            substations=substations,
            sort_params=sort_params,
            exclude_expired=exclude_expired,
            table_code=table_code,
            from_id=from_id,
            limit=limit
        )
        return result

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        solicitation = yield self.store.get_solicitation_by_id(id=id)
        return solicitation

    @defer.inlineCallbacks
    def change_solicitation_status(self, new_status, id, user_id):
        ts = calendar.timegm(time.gmtime())
        yield self.store.create_solicitation_status_signature(id, user_id, new_status, ts)

        token = yield self.store.create_solicitation_updated_event(
            EventTypes.ChangeSolicitationStatus, id, user_id, {"status": new_status}
        )
        self.notifier.on_new_event("solicitations_key", token)

    @defer.inlineCallbacks
    def check_substation(self, company_code, substation):
        substation_object = yield self.substation_handler. \
            get_substation_by_company_code_and_substation_code(
                company_code, 
                substation
            )
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
