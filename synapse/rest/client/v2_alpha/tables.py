import logging

from synapse.rest.admin._base import assert_requester_is_admin
from synapse.api.errors import SynapseError
from synapse.http.servlet import (RestServlet,
                                  parse_json_object_from_request)
from ._base import client_patterns
from twisted.internet import defer

logger = logging.getLogger(__name__)


class TableServlet(RestServlet):
    PATTERNS = client_patterns("/associate_tables/(?P<user_id>[^/]*)$")

    def __init__(self, hs):
        """
        Args:
            hs (synapse.server.HomeServer): server
        """
        super(TableServlet, self).__init__()

        self.hs = hs
        self.auth = hs.get_auth()
        self.table_handler = hs.get_table_handler()

    @defer.inlineCallbacks
    def on_PUT(self, request, user_id):
        yield assert_requester_is_admin(self.auth, request)
        content = parse_json_object_from_request(request)

        if "tables" in content and content["tables"]:
            yield self.table_handler.associate_tables_to_user(user_id, content["tables"])
            return 200, "The tables were associated with the user!"
        else:
            raise SynapseError(400, "Empty tables!")


def register_servlets(hs, http_server):
    TableServlet(hs).register(http_server)
