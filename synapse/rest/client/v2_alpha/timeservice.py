from ._base import client_patterns
from synapse.http.servlet import RestServlet
from twisted.internet import defer


class TimeRestServlet(RestServlet):

    PATTERNS = client_patterns("/current_time$")

    def __init__(self, hs):

        super(TimeRestServlet, self).__init__()

        self.hs = hs
        self.auth = hs.get_auth()

    @defer.inlineCallbacks
    def on_GET(self, request):

        """
        :param request:
        :return: current time in miliseconds.
        """
        yield self.auth.get_user_by_req(request)

        current_time = int(self.hs.get_clock().time_msec())
        return 200, {'timestamp': current_time}


def register_servlets(hs, http_server):
    TimeRestServlet(hs).register(http_server)
