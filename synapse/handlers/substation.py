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


class SubstationHandler(BaseHandler):

    def __init__(self, hs):
        self.hs = hs
        self.store = hs.get_datastore()

    @defer.inlineCallbacks
    def get_substations(self):
        substations = yield self.store.get_substations()
        return substations

    @defer.inlineCallbacks
    def get_substation_by_company_code_and_substation_code(self, company_code, substation_code):
        solicitation = yield self.store.get_substation_by_company_code_and_substation_code(
            company_code, substation_code)
        return solicitation