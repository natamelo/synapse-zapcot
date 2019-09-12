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
from synapse.api.constants import SolicitationStatus

import calendar;
import time;

logger = logging.getLogger(__name__)


class VoltageControlHandler(BaseHandler):

    def __init__(self, hs):
        super(VoltageControlHandler, self).__init__(hs)
        self.hs = hs
        self.store = hs.get_datastore()
        self.state = hs.get_state_handler()
        self.event_creation_handler = hs.get_event_creation_handler()

    @defer.inlineCallbacks
    def create_solicitation(self, action, equipment, substation, bar, value, userId):
        ts = calendar.timegm(time.gmtime())
        status = SolicitationStatus.NOT_ANSWERED
        yield self.store.create_solicitation(action=action, equipment=equipment, substation=substation, 
        bar=bar, userId=userId, ts=ts, status=status, value=value)

    @defer.inlineCallbacks
    def get_substations_codes(self):
        substations = yield self.store.get_substations()
        substations_codes = []
        for sub in substations:
            substations_codes.append(sub['code'])
        return substations_codes

    @defer.inlineCallbacks
    def get_solicitation_by_id(self, id):
        solicitation = yield self.store.get_solicitation_by_id(id=id)
        return solicitation

    @defer.inlineCallbacks
    def change_solicitation_status(self, new_status, id):
        yield self.store.change_solicitation_status(new_status, id)