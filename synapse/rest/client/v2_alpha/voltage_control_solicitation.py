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

from synapse.events.utils import format_event_for_client_v2_without_room_id
from synapse.http.servlet import RestServlet, parse_integer, parse_string, parse_json_object_from_request
from synapse.api.constants import SolicitationStatus, SolicitationActions, Companies, EquipmentTypes

from ._base import client_patterns

logger = logging.getLogger(__name__)


class VoltageControlSolicitationServlet(RestServlet):
    PATTERNS = client_patterns("/voltage_control_solicitation$")

    def __init__(self, hs):
        super(VoltageControlSolicitationServlet, self).__init__()
        self.store = hs.get_datastore()
        self.auth = hs.get_auth()
        self.clock = hs.get_clock()
        self._event_serializer = hs.get_event_client_serializer()
        self._voltage_control_handler = hs.get_voltage_control_handler()

    @defer.inlineCallbacks
    def on_POST(self, request):

        requester = yield self.auth.get_user_by_req(request)
        userId = requester.user.to_string()
        company_code = requester.company_code

        if company_code != Companies.ONS:
            return (401, "Permission denied.")
        
        body = parse_json_object_from_request(request)
        action = body['action']
        equipment = body['equipment']
        substation = body['substation']
        bar = body['bar']
        value = body['value']
        
        if action not in SolicitationActions.ALL_ACTIONS:
            return (400, "Action must be valid.")
        if equipment not in EquipmentTypes.ALL_EQUIPMENT:
            return (400, "Equipment type must be valid.")

        subs_codes = yield self._voltage_control_handler.get_substations_codes(self=self)
        if substation not in subs_codes:
            return (400, "Substation code must be valid.")

        yield self._voltage_control_handler.create_solicitation(self=self, action=action,
        equipment=equipment, substation=substation, bar=bar, value=value, userId=userId)

        return (201, "Voltage control solicitation created with success.")

def register_servlets(hs, http_server):
    VoltageControlSolicitationServlet(hs).register(http_server)
