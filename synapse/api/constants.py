# -*- coding: utf-8 -*-
# Copyright 2014-2016 OpenMarket Ltd
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

"""Contains constants from the specification."""

# the "depth" field on events is limited to 2**63 - 1
MAX_DEPTH = 2 ** 63 - 1

# the maximum length for a room alias is 255 characters
MAX_ALIAS_LENGTH = 255

# the maximum length for a user id is 255 characters
MAX_USERID_LENGTH = 255


class Membership(object):

    """Represents the membership states of a user in a room."""

    INVITE = "invite"
    JOIN = "join"
    KNOCK = "knock"
    LEAVE = "leave"
    BAN = "ban"
    LIST = (INVITE, JOIN, KNOCK, LEAVE, BAN)


class PresenceState(object):
    """Represents the presence state of a user."""

    OFFLINE = "offline"
    UNAVAILABLE = "unavailable"
    ONLINE = "online"


class JoinRules(object):
    PUBLIC = "public"
    KNOCK = "knock"
    INVITE = "invite"
    PRIVATE = "private"


class LoginType(object):
    PASSWORD = "m.login.password"
    EMAIL_IDENTITY = "m.login.email.identity"
    MSISDN = "m.login.msisdn"
    RECAPTCHA = "m.login.recaptcha"
    TERMS = "m.login.terms"
    DUMMY = "m.login.dummy"

    # Only for C/S API v1
    APPLICATION_SERVICE = "m.login.application_service"
    SHARED_SECRET = "org.matrix.login.shared_secret"


class EventTypes(object):
    Member = "m.room.member"
    Create = "m.room.create"
    Tombstone = "m.room.tombstone"
    JoinRules = "m.room.join_rules"
    PowerLevels = "m.room.power_levels"
    Aliases = "m.room.aliases"
    Redaction = "m.room.redaction"
    ThirdPartyInvite = "m.room.third_party_invite"
    Encryption = "m.room.encryption"
    RelatedGroups = "m.room.related_groups"

    RoomHistoryVisibility = "m.room.history_visibility"
    CanonicalAlias = "m.room.canonical_alias"
    Encryption = "m.room.encryption"
    RoomAvatar = "m.room.avatar"
    RoomEncryption = "m.room.encryption"
    GuestAccess = "m.room.guest_access"

    # These are used for validation
    Message = "m.room.message"
    Topic = "m.room.topic"
    Name = "m.room.name"

    ServerACL = "m.room.server_acl"
    Pinned = "m.room.pinned_events"

    # There are used for Solicitations
    CreateSolicitation = "m.solicitation.create"
    ChangeSolicitationStatus = "m.solicitation.change_status"


class RejectedReason(object):
    AUTH_ERROR = "auth_error"
    REPLACED = "replaced"
    NOT_ANCESTOR = "not_ancestor"


class RoomCreationPreset(object):
    PRIVATE_CHAT = "private_chat"
    PUBLIC_CHAT = "public_chat"
    TRUSTED_PRIVATE_CHAT = "trusted_private_chat"


class ThirdPartyEntityKind(object):
    USER = "user"
    LOCATION = "location"


ServerNoticeMsgType = "m.server_notice"
ServerNoticeLimitReached = "m.server_notice.usage_limit_reached"


class UserTypes(object):
    """Allows for user type specific behaviour. With the benefit of hindsight
    'admin' and 'guest' users should also be UserTypes. Normal users are type None
    """

    SUPPORT = "support"
    ALL_USER_TYPES = (SUPPORT,)


class RelationTypes(object):
    """The types of relations known to this server.
    """

    ANNOTATION = "m.annotation"
    REPLACE = "m.replace"
    REFERENCE = "m.reference"


class SolicitationStatus(object):
    """The possible status.
    """

    NEW = "NEW"
    ACCEPTED = "ACCEPTED"
    EXECUTED = "EXECUTED"
    CANCELED = "CANCELED"
    LATE = "LATE"
    CONTESTED = "CONTESTED"
    BLOCKED = "BLOCKED"
    REQUIRED = "REQUIRED"
    ALL_SOLICITATION_TYPES = [NEW, ACCEPTED, EXECUTED,
                              CANCELED, LATE, CONTESTED,
                              BLOCKED, REQUIRED]


class Companies(object):
    """The companies.
    """

    ONS = "ONS"
    CHESF = "CHESF"
    CTEEP = "CTEEP"
    ALL_COMPANIES = [ONS, CHESF, CTEEP]

    ALL_COMPANIES = (ONS, CTEEP, CHESF)


class SolicitationActions(object):
    """The possible actions.
    """
    # Ligar
    TURN_ON = "TURN_ON"
    # Desligar
    TURN_OFF = "TURN_OFF"
    # Elevar
    RISE = "RISE"
    # Reduzir
    REDUCE = "REDUCE"
    # Maximizar
    MAXIMIZE = "MAXIMIZE"
    # Minimizar
    MINIMIZE = "MINIMIZE"
    # Ajustar
    ADJUST = "ADJUST"
    # Ajustar Tape
    ADJUST_FOR_TAPE = "ADJUST_FOR_TAPE"
    # Zerar
    RESET = "RESET"
    ALL_ACTIONS = [TURN_ON, TURN_OFF, RISE, REDUCE, MINIMIZE, MAXIMIZE, ADJUST, RESET, ADJUST_FOR_TAPE]


class EquipmentTypes(object):
    """The equipment types.
    """

    CAPACITOR = "CAPACITOR"
    REACTOR = "REACTOR"
    SYNCHRONOUS = "SYNCHRONOUS"
    TRANSFORMER = "TRANSFORMER"
    ALL_EQUIPMENT = [CAPACITOR, REACTOR, SYNCHRONOUS, TRANSFORMER]


class VoltageTransformerLevels(object):
    """The voltage levels for Voltage Transformer.
    """
    _500kV = "500kV"
    _440kV = "440kV"
    _345kV = "345kV"
    _230kV = "230kV"
    _138kV = "138kV"
    _88kV = "88kV"
    ALL_ALLOWED_LEVELS = [_500kV, _440kV, _345kV, _230kV, _138kV, _88kV]


class SolicitationSortParams(object):
    """The possible params to sort voltage control solicitations.
    """

    CREATION_TIME = "creation_time"
    STATUS = "status"
    SUBSTATION = "substation"
    ALL_PARAMS = [CREATION_TIME, STATUS, SUBSTATION]
