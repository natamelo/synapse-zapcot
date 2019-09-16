"""Contains functions for manage tables."""

import logging

from ._base import BaseHandler

from twisted.internet import defer
from synapse.api.errors import (SynapseError, Codes)

logger = logging.getLogger(__name__)


class TableHandler(BaseHandler):
    def __init__(self, hs):
        """

        Args:
            hs (synapse.server.HomeServer):
        """
        super(TableHandler, self).__init__(hs)
        self.hs = hs
        self.store = hs.get_datastore()

    @defer.inlineCallbacks
    def associate_tables_to_user(self, user_id, tables):

        users = yield self.store.get_users_by_id_case_insensitive(user_id)
        if not users:
            raise SynapseError(
                400, "User not found.", Codes.BAD_JSON
            )

        for code in tables:
            result = yield self.store.get_table_by_code(code)
            if not result:
                raise SynapseError(
                    400, "One or more invalid table!", Codes.BAD_JSON
                )

        for code in tables:
            self.store.associate_table_to_user(user_id, code)

    @defer.inlineCallbacks
    def filter_tables_by_company_code(self, company_code):
        tables = yield self.store.get_tables_by_company_code(company_code)
        return tables
