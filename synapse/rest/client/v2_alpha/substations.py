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


class SubstationServlet(RestServlet):
    PATTERNS = client_patterns("/substations$")

    def __init__(self, hs):
        super(SubstationServlet, self).__init__()

        self.hs = hs
        self.auth = hs.get_auth()
        self.substation_handler = hs.get_substation_handler()

    @defer.inlineCallbacks
    def on_GET(self, request):
        requester = yield self.auth.get_user_by_req(request)

        substations = yield self.substation_handler.get_substations()
        return(200, substations)



def register_servlets(hs, http_server):
    SubstationServlet(hs).register(http_server)
