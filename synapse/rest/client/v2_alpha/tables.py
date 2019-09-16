import logging

from synapse.api.constants import Companies
from synapse.rest.admin._base import assert_requester_is_admin
from synapse.api.errors import SynapseError
from synapse.http.servlet import (RestServlet,
                                  parse_json_object_from_request)

from synapse.api.errors import Codes
from ._base import client_patterns
from twisted.internet import defer

logger = logging.getLogger(__name__)


class AssociateTableServlet(RestServlet):
    PATTERNS = client_patterns("/associate_tables/(?P<user_id>[^/]*)$")

    def __init__(self, hs):
        """
        Args:
            hs (synapse.server.HomeServer): server
        """
        super(AssociateTableServlet, self).__init__()

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


class FilterTableServlet(RestServlet):
    PATTERNS = client_patterns("/tables$")

    def __init__(self, hs):
        """
        Args:
            hs (synapse.server.HomeServer): server
        """
        super(FilterTableServlet, self).__init__()

        self.hs = hs
        self.auth = hs.get_auth()
        self.table_handler = hs.get_table_handler()

    @defer.inlineCallbacks
    def on_GET(self, request):
        requester = yield self.auth.get_user_by_req(request)
        user_company_code = requester.company_code

        company_code = None
        company_code_params = request.args.get(b"company_code")
        if company_code_params is not None:
            company_code = company_code_params[0].decode("ascii")
            if company_code not in Companies.ALL_COMPANIES:
                raise SynapseError(404, "Company not found", Codes.NOT_FOUND)
            elif user_company_code != company_code:
                raise SynapseError(403, "User can only access the solicitations of your company", Codes.FORBIDDEN)
        else:
            raise SynapseError(400, "It's necessary the param company code", Codes.INVALID_PARAM)

        tables = yield self.table_handler.filter_tables_by_company_code(company_code)

        return 200, tables


def register_servlets(hs, http_server):
    AssociateTableServlet(hs).register(http_server)
    FilterTableServlet(hs).register(http_server)
