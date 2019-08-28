# -*- coding: utf-8 -*-
# Copyright 2015, 2016 OpenMarket Ltd
# Copyright 2017 Vector Creations Ltd
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

"""Utilities for interacting with Identity Servers"""

import logging

from canonicaljson import json

from twisted.internet import defer

from synapse.api.errors import (
    CodeMessageException,
    Codes,
    HttpResponseException,
    SynapseError,
)

from ._base import BaseHandler

logger = logging.getLogger(__name__)


class IdentityHandler(BaseHandler):
    def __init__(self, hs):
        super(IdentityHandler, self).__init__(hs)

        self.http_client = hs.get_simple_http_client()
        self.federation_http_client = hs.get_http_client()

        self.trusted_id_servers = set(hs.config.trusted_third_party_id_servers)
        self.trust_any_id_server_just_for_testing_do_not_use = (
            hs.config.use_insecure_ssl_client_just_for_testing_do_not_use
        )

    def _should_trust_id_server(self, id_server):
        if id_server not in self.trusted_id_servers:
            if self.trust_any_id_server_just_for_testing_do_not_use:
                logger.warn(
                    "Trusting untrustworthy ID server %r even though it isn't"
                    " in the trusted id list for testing because"
                    " 'use_insecure_ssl_client_just_for_testing_do_not_use'"
                    " is set in the config",
                    id_server,
                )
            else:
                return False
        return True

    @defer.inlineCallbacks
    def threepid_from_creds(self, creds, use_v2=True):
        """
        Retrieve a threepid identifier from a "credentials" dictionary

        Args:
            creds (dict[str, str]): Dictionary of credentials that contain the following keys:
                * client_secret|clientSecret: A unique secret str provided by the client
                * id_server|idServer: the domain of the identity server to query
                * id_access_token: The access token to authenticate to the identity
                    server with. Required if use_v2 is true
            use_v2 (bool): Whether to use v2 Identity Service API endpoints

        Returns:
            Deferred[dict[str,str|int]|None]: A dictionary consisting of response params to
                the /getValidated3pid endpoint of the Identity Service API, or None if the
                threepid was not found
        """
        client_secret = creds.get("client_secret") or creds.get("clientSecret")
        if not client_secret:
            raise SynapseError(400, "No client_secret in creds")

        id_server = creds.get("id_server") or creds.get("idServer")
        if not id_server:
            raise SynapseError(400, "No id_server in creds")

        if use_v2:
            # v2 endpoints require an identity server access token. We need one if we're
            # using v2 endpoints
            id_access_token = creds.get("id_access_token")
            if not id_access_token:
                raise SynapseError(400, "No id_access_token in creds when id_server provided")

            # Use the v2 API endpoint URLs
            url_endpoint = "/_matrix/identity/v2/3pid/getValidated3pid"
        else:
            # Use the v1 API endpoint URLs
            url_endpoint = "/_matrix/identity/api/v1/3pid/getValidated3pid"

        if not self._should_trust_id_server(id_server):
            logger.warn(
                "%s is not a trusted ID server: rejecting 3pid " + "credentials",
                id_server,
            )
            return None

        try:
            data = yield self.http_client.get_json(
                "https://%s%s"
                % (id_server, url_endpoint),
                {"sid": creds["sid"], "client_secret": client_secret},
            )
        except HttpResponseException as e:
            if e.code is 404 and use_v2:
                # This identity server is too old to understand Identity Service API v2
                # Attempt v1 endpoint
                return (yield self.threepid_from_creds(creds, use_v2=False))

            logger.info("getValidated3pid failed with Matrix error: %r", e)
            raise e.to_synapse_error()

        return data if "medium" in data else None

    @defer.inlineCallbacks
    def bind_threepid(self, creds, mxid):
        logger.debug("binding threepid %r to %s", creds, mxid)

        if "id_server" in creds:
            id_server = creds["id_server"]
        elif "idServer" in creds:
            id_server = creds["idServer"]
        else:
            raise SynapseError(400, "No id_server in creds")

        if "client_secret" in creds:
            client_secret = creds["client_secret"]
        elif "clientSecret" in creds:
            client_secret = creds["clientSecret"]
        else:
            raise SynapseError(400, "No client_secret in creds")

        try:
            data = yield self.http_client.post_json_get_json(
                "https://%s%s" % (id_server, "/_matrix/identity/api/v1/3pid/bind"),
                {"sid": creds["sid"], "client_secret": client_secret, "mxid": mxid},
            )
            logger.debug("bound threepid %r to %s", creds, mxid)

            # Remember where we bound the threepid
            yield self.store.add_user_bound_threepid(
                user_id=mxid,
                medium=data["medium"],
                address=data["address"],
                id_server=id_server,
            )
        except CodeMessageException as e:
            data = json.loads(e.msg)  # XXX WAT?
        return data

    @defer.inlineCallbacks
    def try_unbind_threepid(self, mxid, threepid):
        """Removes a binding from an identity server

        Args:
            mxid (str): Matrix user ID of binding to be removed
            threepid (dict): Dict with medium & address of binding to be
                removed, and an optional id_server.

        Raises:
            SynapseError: If we failed to contact the identity server

        Returns:
            Deferred[bool]: True on success, otherwise False if the identity
            server doesn't support unbinding (or no identity server found to
            contact).
        """
        if threepid.get("id_server"):
            id_servers = [threepid["id_server"]]
        else:
            id_servers = yield self.store.get_id_servers_user_bound(
                user_id=mxid, medium=threepid["medium"], address=threepid["address"]
            )

        # We don't know where to unbind, so we don't have a choice but to return
        if not id_servers:
            return False

        changed = True
        for id_server in id_servers:
            changed &= yield self.try_unbind_threepid_with_id_server(
                mxid, threepid, id_server
            )

        return changed

    @defer.inlineCallbacks
    def try_unbind_threepid_with_id_server(self, mxid, threepid, id_server, use_v2=True):
        """Removes a binding from an identity server

        Args:
            mxid (str): Matrix user ID of binding to be removed
            threepid (dict): Dict with medium & address of binding to be removed
            id_server (str): Identity server to unbind from
            use_v2 (bool): Whether to use the v2 identity service unbind API

        Raises:
            SynapseError: If we failed to contact the identity server

        Returns:
            Deferred[bool]: True on success, otherwise False if the identity
            server doesn't support unbinding
        """
        # First attempt the v2 endpoint
        if use_v2:
            url = "https://%s/_matrix/identity/v2/3pid/unbind" % (id_server,)
            url_bytes = "/_matrix/identity/v2/3pid/unbind".encode("ascii")
        else:
            url = "https://%s/_matrix/identity/api/v1/3pid/unbind" % (id_server,)
            url_bytes = "/_matrix/identity/api/v1/3pid/unbind".encode("ascii")

        content = {
            "mxid": mxid,
            "threepid": {"medium": threepid["medium"], "address": threepid["address"]},
        }

        # we abuse the federation http client to sign the request, but we have to send it
        # using the normal http client since we don't want the SRV lookup and want normal
        # 'browser-like' HTTPS.
        auth_headers = self.federation_http_client.build_auth_headers(
            destination=None,
            method="POST",
            url_bytes=url_bytes,
            content=content,
            destination_is=id_server,
        )
        headers = {b"Authorization": auth_headers}

        try:
            yield self.http_client.post_json_get_json(url, content, headers)
            changed = True
        except HttpResponseException as e:
            changed = False
            if e.code is 404 and use_v2:
                # v2 is not supported yet, try again with v1
                return (yield self.try_unbind_threepid_with_id_server(
                    mxid, threepid, id_server, use_v2=False
                ))
            elif e.code in (400, 404, 501):
                # The remote server probably doesn't support unbinding (yet)
                logger.warn("Received %d response while unbinding threepid", e.code)
            else:
                logger.error("Failed to unbind threepid on identity server: %s", e)
                raise SynapseError(502, "Failed to contact identity server")

        yield self.store.remove_user_bound_threepid(
            user_id=mxid,
            medium=threepid["medium"],
            address=threepid["address"],
            id_server=id_server,
        )

        return changed

    @defer.inlineCallbacks
    def requestEmailToken(
        self, id_server, email, client_secret, send_attempt, next_link=None
    ):
        if not self._should_trust_id_server(id_server):
            raise SynapseError(
                400, "Untrusted ID server '%s'" % id_server, Codes.SERVER_NOT_TRUSTED
            )

        params = {
            "email": email,
            "client_secret": client_secret,
            "send_attempt": send_attempt,
        }

        if next_link:
            params.update({"next_link": next_link})

        try:
            data = yield self.http_client.post_json_get_json(
                "https://%s%s"
                % (id_server, "/_matrix/identity/api/v1/validate/email/requestToken"),
                params,
            )
            return data
        except HttpResponseException as e:
            logger.info("Proxied requestToken failed: %r", e)
            raise e.to_synapse_error()

    @defer.inlineCallbacks
    def requestMsisdnToken(
        self, id_server, country, phone_number, client_secret, send_attempt, **kwargs
    ):
        if not self._should_trust_id_server(id_server):
            raise SynapseError(
                400, "Untrusted ID server '%s'" % id_server, Codes.SERVER_NOT_TRUSTED
            )

        params = {
            "country": country,
            "phone_number": phone_number,
            "client_secret": client_secret,
            "send_attempt": send_attempt,
        }
        params.update(kwargs)

        try:
            data = yield self.http_client.post_json_get_json(
                "https://%s%s"
                % (id_server, "/_matrix/identity/api/v1/validate/msisdn/requestToken"),
                params,
            )
            return data
        except HttpResponseException as e:
            logger.info("Proxied requestToken failed: %r", e)
            raise e.to_synapse_error()
