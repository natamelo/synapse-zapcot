# -*- coding: utf-8 -*-
# Copyright 2016 OpenMarket Ltd
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

from twisted.internet import defer

from synapse.http.servlet import RestServlet, parse_integer, parse_string, parse_json_object_from_request
from synapse.api.constants import SolicitationStatus, SolicitationActions, Companies, EquipmentTypes

from synapse.api.errors import SynapseError

from ._base import client_patterns

from synapse.api.errors import (
    Codes,
    InvalidClientTokenError,
    SynapseError,
)

import calendar;
import time;

logger = logging.getLogger(__name__)


class VoltageControlSolicitationServlet(RestServlet):
    PATTERNS = client_patterns("/voltage_control_solicitation$")

    def __init__(self, hs):
        super(VoltageControlSolicitationServlet, self).__init__()
        self.store = hs.get_datastore()
        self.auth = hs.get_auth()
        self._voltage_control_handler = hs.get_voltage_control_handler()

    @defer.inlineCallbacks
    def on_POST(self, request):

        requester = yield self.auth.get_user_by_req(request)
        userId = requester.user.to_string()
        company_code = requester.company_code

        if company_code != Companies.ONS:
            raise InvalidClientTokenError(401, "User should to belong ONS.")
        
        body = parse_json_object_from_request(request)
        action = body['action']
        equipment = body['equipment']
        substation = body['substation']
        bar = body['bar']
        value = body['value']
        
        if action not in SolicitationActions.ALL_ACTIONS:
            raise SynapseError(400, "Invalid action!", Codes.INVALID_PARAM)
        if equipment not in EquipmentTypes.ALL_EQUIPMENT:
            raise SynapseError(400, "Invalid Equipment!", Codes.INVALID_PARAM)

        codes = yield self._voltage_control_handler.get_substations_codes(self=self)
        if substation not in codes:
            raise SynapseError(400, "Invalid substation!", Codes.INVALID_PARAM)

        yield self._voltage_control_handler.create_solicitation(self=self, action=action,
        equipment=equipment, substation=substation, bar=bar, value=value, userId=userId)

        return (201, "Voltage control solicitation created with success.")

class VoltageControlStatusServlet(RestServlet):
    PATTERNS = client_patterns("/voltage_control_solicitation/(?P<solicitation_id>[^/]*)")

    def __init__(self, hs):
        super(VoltageControlStatusServlet, self).__init__()
        self.store = hs.get_datastore()
        self.auth = hs.get_auth()
        self._voltage_control_handler = hs.get_voltage_control_handler()

    @defer.inlineCallbacks
    def on_PUT(self, request, solicitation_id):

        requester = yield self.auth.get_user_by_req(request)
        user_id = requester.user.to_string()
        company_code = requester.company_code

        body = parse_json_object_from_request(request)
        new_status = body["status"]
        if new_status not in SolicitationStatus.ALL_SOLICITATION_TYPES:
            raise SynapseError(400, "Invalid status.", Codes.INVALID_PARAM)

        solicitation = yield self._voltage_control_handler.get_solicitation_by_id(self=self, id=solicitation_id)
        if not solicitation:
            raise SynapseError(404, "Solicitation was not found.", Codes.NOT_FOUND)

        atual_status = solicitation["status"]
        creation_ts = solicitation["creation_timestamp"]

        if self._validate_status_change(atual_status, new_status, company_code, creation_ts):
            yield self._voltage_control_handler.change_solicitation_status(self=self, new_status=new_status, id=solicitation_id, user_id=user_id)
            return (200, {"message": "Solicitation status changed."})

    def _validate_creation_time(self, creation_ts):
        
        result = calendar.timegm(time.gmtime()) - creation_ts
        return result <= 300 # 300 = 5 minutes in timestamp

    def _validate_status_change(self, atual_status, new_status, company_code, creation_ts):
        
        if new_status == SolicitationStatus.AWARE:
            if company_code == Companies.ONS:
                raise InvalidClientTokenError(401, "Permission denied")
            elif atual_status != SolicitationStatus.NOT_ANSWERED:
                raise SynapseError(400, "Inconsistent status.", Codes.INVALID_PARAM)
            elif not self._validate_creation_time(creation_ts):
                raise SynapseError(400, "Solicitation expired.", Codes.LIMIT_EXCEEDED)
            else:
                return True
        elif new_status == SolicitationStatus.ANSWERED:
            if company_code == Companies.ONS:
                raise InvalidClientTokenError(401, "Permission denied")
            elif atual_status != SolicitationStatus.AWARE:
                raise SynapseError(400, "Inconsistent status.", Codes.INVALID_PARAM)
            else:
                return True
        elif new_status == SolicitationStatus.CANCELED:
            if company_code != Companies.ONS:
                raise InvalidClientTokenError(401, "Permission denied")
            elif atual_status != SolicitationStatus.NOT_ANSWERED:
                raise SynapseError(400, "Inconsistent status.", Codes.INVALID_PARAM)
            else:
                return True
        elif new_status == SolicitationStatus.EXPIRED:
            raise SynapseError(400, "Inconsistent status.", Codes.INVALID_PARAM)
        elif new_status == SolicitationStatus.RETURNED:
            if company_code == Companies.ONS:
                raise InvalidClientTokenError(401, "Permission denied")
            elif atual_status != SolicitationStatus.ANSWERED:
                raise SynapseError(400, "Inconsistent status.", Codes.INVALID_PARAM)
            else:
                return True

def register_servlets(hs, http_server):
    VoltageControlSolicitationServlet(hs).register(http_server)
    VoltageControlStatusServlet(hs).register(http_server)
