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

from ._base import client_patterns

from synapse.api.errors import (
    Codes,
    InvalidClientTokenError,
    SynapseError,
)

logger = logging.getLogger(__name__)


class VoltageControlSolicitationServlet(RestServlet):
    PATTERNS = client_patterns("/voltage_control_solicitation$")

    def __init__(self, hs):
        super(VoltageControlSolicitationServlet, self).__init__()

        self.hs = hs
        self.auth = hs.get_auth()
        self.voltage_control_handler = hs.get_voltage_control_handler()

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

        codes = yield self.voltage_control_handler.get_substation_codes()
        if substation not in codes:
            raise SynapseError(400, "Invalid substation!", Codes.INVALID_PARAM)

        yield self.voltage_control_handler.create_solicitation(action=action,
            equipment=equipment, substation=substation, bar=bar, value=value, userId=userId)

        return (201, "Voltage control solicitation created with success.")

    @defer.inlineCallbacks
    def on_GET(self, request):
        requester = yield self.auth.get_user_by_req(request)
        user_company_code = requester.company_code

        limit = min(parse_integer(request, "limit", default=50), 100)
        from_solicitation_id = parse_integer(request, "from_id", default=0)
        company_code = parse_string(request, "company_code", default=None)

        if company_code is not None:
            if company_code not in Companies.ALL_COMPANIES:
                raise SynapseError(404, "Company not found", Codes.NOT_FOUND)
            elif user_company_code != Companies.ONS and user_company_code != company_code:
                raise SynapseError(403, "User can only access the solicitations of your company", Codes.FORBIDDEN)
        else:
            raise SynapseError(400, "Company code not informed", Codes.INVALID_PARAM)
        result = yield self.voltage_control_handler.filter_solicitations(company_code=company_code,
                                                                         from_id=from_solicitation_id,
                                                                         limit=limit)
        return 200, result


class VoltageControlStatusServlet(RestServlet):
    PATTERNS = client_patterns("/voltage_control_solicitation/(?P<solicitation_id>[^/]*)")

    def __init__(self, hs):
        super(VoltageControlStatusServlet, self).__init__()

        self.hs = hs
        self.auth = hs.get_auth()
        self.event_serializer = hs.get_event_client_serializer()
        self.voltage_control_handler = hs.get_voltage_control_handler()

    def on_PUT(self, request, solicitation_id):

        return (200, solicitation_id)


def register_servlets(hs, http_server):
    VoltageControlSolicitationServlet(hs).register(http_server)
    VoltageControlStatusServlet(hs).register(http_server)
