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
from synapse.types import create_requester
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


import calendar;
import time;

logger = logging.getLogger(__name__)


class VoltageControlHandler(BaseHandler):

    def __init__(self, hs):
        self.hs = hs
        self.substation_handler = hs.get_substation_handler()
        self.store = hs.get_datastore()

    @defer.inlineCallbacks
    def create_solicitations(self, solicitations, user_id):
        status = SolicitationStatus.NOT_ANSWERED

        for solicitation in solicitations:
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
        yield self.store.create_solicitation_event(id, user_id, new_status, ts)

    @defer.inlineCallbacks
    def check_substation(self, company_code, substation):
        substation_object = yield self.substation_handler. \
            get_substation_by_company_code_and_substation_code(
                company_code, 
                substation
            )
        if substation_object is None:
            raise SynapseError(400, "Invalid substation!", Codes.INVALID_PARAM)


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
    if "voltage" not in solicitation:
        solicitation["voltage"] = None
    check_action_type(
        action=solicitation["action"],
        possible_actions=[SolicitationActions.LIGAR, SolicitationActions.DESLIGAR],
        equipment_type=solicitation["equipment"]
    )
    check_amount(
        amount=solicitation["amount"],
        min_value=0,
        equipment_type=solicitation["equipment"]
    )
    check_staggered(
        staggered=solicitation["staggered"],
        equipment_type=solicitation["equipment"]
    )


def check_capacitor_params(solicitation):
    solicitation["voltage"] = None
    check_action_type(
        action=solicitation["action"],
        possible_actions=[SolicitationActions.LIGAR, SolicitationActions.DESLIGAR],
        equipment_type=solicitation["equipment"]
    )
    check_amount(
        amount=solicitation["amount"],
        min_value=0,
        equipment_type=solicitation["equipment"]
    )
    check_staggered(
        staggered=solicitation["staggered"],
        equipment_type=solicitation["equipment"]
    )


def check_transform_params(solicitation):
    solicitation["staggered"] = None
    check_action_type(
        action=solicitation["action"],
        possible_actions=[SolicitationActions.ELEVAR, SolicitationActions.REDUZIR],
        equipment_type=solicitation["equipment"]
    )
    check_amount(
        amount=solicitation["amount"],
        min_value=0,
        equipment_type=solicitation["equipment"]
    )

    if "voltage" not in solicitation or solicitation["voltage"] == "":
        raise SynapseError(
            400,
            "Voltage value must be informed for 'TRANSFORMADOR'.",
            Codes.INVALID_PARAM
        )
    elif solicitation["voltage"] not in VoltageTransformerLevels.ALL_ALLOWED_LEVELS:
        raise SynapseError(
            400,
            "Invalid voltage value for equipment type 'TRANSFORMADOR'.",
            Codes.INVALID_PARAM
        )


def check_synchronous_params(solicitation):
    solicitation["staggered"] = None
    if "voltage" not in solicitation:
        solicitation["voltage"] = None

    if solicitation["action"] == SolicitationActions.AJUSTAR:
        if "amount" not in solicitation or solicitation["amount"] == "":
            raise  SynapseError(
                400,
                "Amount must be informed for action 'AJUSTAR' on equipment type 'SINCRONO'",
                Codes.INVALID_PARAM
            )
        else:
            check_amount(
                amount=solicitation["amount"],
                min_value=0,
                equipment_type=solicitation["equipment"]
            )
    else:
        check_action_type(
            action=solicitation["action"],
            possible_actions=[SolicitationActions.MAXIMIZAR, SolicitationActions.ZERAR],
            equipment_type=solicitation["equipment"]
        )
        solicitation["amount"] = None


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
                "Invalid amount value for equipment type '%s'." % (equipment_type),
                Codes.INVALID_PARAM
            )
    except Exception as e:
        raise SynapseError(
            400,
            "Invalid amount value for equipment type '%s'." % (equipment_type),
            Codes.INVALID_PARAM
        )


def check_staggered(staggered, equipment_type):
    if type(staggered) != bool:
        raise SynapseError(
            400,
            "Invalid staggered for equipment type '%s'." % (equipment_type),
            Codes.INVALID_PARAM
        )