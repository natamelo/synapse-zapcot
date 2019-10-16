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

from synapse.http.servlet import RestServlet, parse_list, parse_integer, parse_string, parse_json_object_from_request
from synapse.api.constants import SolicitationStatus, SolicitationActions, Companies, EquipmentTypes, SolicitationSortParams

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

        self.hs = hs
        self.auth = hs.get_auth()
        self.voltage_control_handler = hs.get_voltage_control_handler()
        self.table_handler = hs.get_table_handler()
        self.substation_handler = hs.get_substation_handler()

    @defer.inlineCallbacks
    def on_POST(self, request):
        requester = yield self.auth.get_user_by_req(request)
        user_id = requester.user.to_string()
        sender_company_code = requester.company_code

        if sender_company_code != Companies.ONS:
            raise InvalidClientTokenError(401, "User should to belong ONS.")
        
        body = parse_json_object_from_request(request)
        solicitations = body['solicitations']

        yield self.voltage_control_handler.create_solicitations(
            solicitations=solicitations,
            user_id=user_id
        )
        return 201, {"message": "Voltage control solicitations created with success."}

    #TODO Resolver prolema de encoding no parm de ordenação "+"
    @defer.inlineCallbacks
    def on_GET(self, request):
        requester = yield self.auth.get_user_by_req(request)
        user_company_code = requester.company_code

        limit = min(parse_integer(request, "limit", default=50), 100)
        from_solicitation_id = parse_integer(request, "from_id", default=0)

        company_code = parse_string(request, "company_code", default=None)
        table_code = parse_string(request, "table_code", default=None)

        substations = parse_list(request, "substations")
        sort_params = parse_list(request, "sort")

        exclude_expired = parse_string(request, "exclude_expired", default=None)

        if company_code is not None:
            if company_code not in Companies.ALL_COMPANIES:
                raise SynapseError(404, "Company not found", Codes.NOT_FOUND)
            elif user_company_code != Companies.ONS and user_company_code != company_code:
                raise SynapseError(403, "User can only access the solicitations of your company", Codes.FORBIDDEN)
        elif user_company_code != Companies.ONS:
            raise SynapseError(400, "Company code not informed", Codes.INVALID_PARAM)

        if table_code is not None:
            table = yield self.table_handler.get_table_by_company_code_and_table_code(company_code, table_code)
            if table is None:
                raise SynapseError(404, "Table not found", Codes.NOT_FOUND)

        if substations:
            for substation_code in substations:
                substation = yield self.substation_handler.\
                    get_substation_by_company_code_and_substation_code(company_code, substation_code)
                if substation is None:
                    raise SynapseError(404, "Substation %r not found" % substation_code, Codes.NOT_FOUND)

        if sort_params:
            for param in sort_params:
                if param not in SolicitationSortParams.ALL_PARAMS:
                    raise SynapseError(400, "Invalid sort param", Codes.INVALID_PARAM)

        result = yield self.voltage_control_handler.filter_solicitations(
            company_code=company_code,
            substations=substations,
            sort_params=sort_params,
            exclude_expired=exclude_expired,
            table_code=table_code,
            from_id=from_solicitation_id,
            limit=limit
        )
        return 200, result


class VoltageControlStatusServlet(RestServlet):
    PATTERNS = client_patterns("/voltage_control_solicitation/(?P<solicitation_id>[^/]*)")

    def __init__(self, hs):
        super(VoltageControlStatusServlet, self).__init__()

        self.hs = hs
        self.auth = hs.get_auth()
        self.voltage_control_handler = hs.get_voltage_control_handler()

    @defer.inlineCallbacks
    def on_PUT(self, request, solicitation_id):

        requester = yield self.auth.get_user_by_req(request)
        user_id = requester.user.to_string()
        user_company_code = requester.company_code

        body = parse_json_object_from_request(request)
        new_status = body["status"]
        if new_status not in SolicitationStatus.ALL_SOLICITATION_TYPES:
            raise SynapseError(400, "Invalid status.", Codes.INVALID_PARAM)

        solicitation = yield self.voltage_control_handler.get_solicitation_by_id(id=solicitation_id)
        if not solicitation:
            raise SynapseError(404, "Solicitation not found.", Codes.NOT_FOUND)

        current_status = solicitation["status"]
        creation_ts = solicitation["creation_timestamp"]

        if self._validate_status_change(current_status, new_status, user_company_code, creation_ts):
            yield self.voltage_control_handler.create_solicitation_event(
                new_status=new_status,
                id=solicitation_id,
                user_id=user_id)
            return (200, {"message": "Solicitation status changed."})

    def _is_valid_create_time(self, creation_ts):

        result = calendar.timegm(time.gmtime()) - creation_ts
        return result <= 300 # 300 = 5 minutes in timestamp

    def _validate_status_change(self, current_status, new_status, user_company_code, creation_ts):

        if new_status == SolicitationStatus.ACCEPTED:
            if user_company_code == Companies.ONS:
                error_message = "Not allowed for users from " + user_company_code
                raise InvalidClientTokenError(401, error_message)
            elif current_status != SolicitationStatus.NEW:
                raise SynapseError(400, "Inconsistent change of status.", Codes.INVALID_PARAM)
            elif not self._is_valid_create_time(creation_ts):
                raise SynapseError(400, "Solicitation expired.", Codes.LIMIT_EXCEEDED)
            else:
                return True
        elif new_status == SolicitationStatus.EXECUTED:
            if user_company_code == Companies.ONS:
                error_message = "Not allowed for users from " + user_company_code
                raise InvalidClientTokenError(401, error_message)
            elif current_status != SolicitationStatus.ACCEPTED:
                raise SynapseError(400, "Inconsistent change of status.", Codes.INVALID_PARAM)
            else:
                return True
        elif new_status == SolicitationStatus.CANCELED:
            if user_company_code != Companies.ONS:
                error_message = "Not allowed for users from " + user_company_code
                raise InvalidClientTokenError(401, error_message)
            elif current_status != SolicitationStatus.NEW:
                raise SynapseError(400, "Inconsistent change of status.", Codes.INVALID_PARAM)
            else:
                return True
        elif new_status == SolicitationStatus.LATE:
            raise SynapseError(400, "Inconsistent change of status.", Codes.INVALID_PARAM)
        elif new_status == SolicitationStatus.CONTESTED:
            if user_company_code == Companies.ONS:
                error_message = "Not allowed for users from " + user_company_code
                raise InvalidClientTokenError(401, error_message)
            elif current_status != SolicitationStatus.EXECUTED:
                raise SynapseError(400, "Inconsistent change of status.", Codes.INVALID_PARAM)
            else:
                return True


def register_servlets(hs, http_server):
    VoltageControlSolicitationServlet(hs).register(http_server)
    VoltageControlStatusServlet(hs).register(http_server)


def check_is_valid_params(params):
    for param in params:
        if param not in SolicitationSortParams.ALL_PARAMS:
            raise SynapseError(400, "Invalid sort param", Codes.INVALID_PARAM)


def get_sort_params(request):
    params = parse_string(request, "sort", default=None)

    if params is not None:
        params_list = params.split("+")
        check_is_valid_params(params)

    return params_list if params else []
