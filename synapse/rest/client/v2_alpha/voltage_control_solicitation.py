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
    InvalidClientTokenError,
    SynapseError,
)

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
            raise InvalidClientTokenError(401, "Permission denied.")
        
        body = parse_json_object_from_request(request)
        action = body['action']
        equipment = body['equipment']
        substation = body['substation']
        bar = body['bar']
        value = body['value']
        
        if action not in SolicitationActions.ALL_ACTIONS:
            raise SynapseError(400, "Action must be valid.")
        if equipment not in EquipmentTypes.ALL_EQUIPMENT:
            raise SynapseError(400, "Equipment type must be valid.")

        subs_codes = yield self._voltage_control_handler.get_substations_codes(self=self)
        if substation not in subs_codes:
            raise SynapseError(400, "Substation code must be valid.")

        yield self._voltage_control_handler.create_solicitation(self=self, action=action,
        equipment=equipment, substation=substation, bar=bar, value=value, userId=userId)

        return (201, "Voltage control solicitation created with success.")

class VoltageControlStatusServlet(RestServlet):
    PATTERNS = client_patterns("/voltage_control_solicitation/(?P<solicitation_id>[^/]*)")

    def __init__(self, hs):
        super(VoltageControlStatusServlet, self).__init__()
        self.store = hs.get_datastore()
        self.auth = hs.get_auth()
        self.clock = hs.get_clock()
        self._event_serializer = hs.get_event_client_serializer()
        self._voltage_control_handler = hs.get_voltage_control_handler()

    def on_PUT(self, request, solicitation_id):

        return (200, solicitation_id)



def register_servlets(hs, http_server):
    VoltageControlSolicitationServlet(hs).register(http_server)
    VoltageControlStatusServlet(hs).register(http_server)
