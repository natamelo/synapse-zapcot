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

    
    def on_POST(self, request):
        body = parse_json_object_from_request(request)

        return (200, body)

def register_servlets(hs, http_server):
    VoltageControlSolicitationServlet(hs).register(http_server)
