import logging

from synapse.storage._base import SQLBaseStore
from twisted.internet import defer

logger = logging.getLogger(__name__)


class TableStore(SQLBaseStore):

    @defer.inlineCallbacks
    def get_table_by_code(self, code):
        """Retrieve table by code.

        Args:
            code (str): The table code, ex: A1.
        """

        result = yield self._simple_select_list(
            table="substations_table",
            keyvalues={"code": code},
            retcols=["code", "name"],
            desc="get_table_by_code",
        )
        return result

    def associate_table_to_user(self, user_id, table_code):

        """Associate table to user.

        Args:
            user_id (str): The User Id, ex: @nata:zapcot.com.
            tables (str): The list of table codes.
        """

        return self._simple_insert(
            table="user_substation_table",
            values={"user_id": user_id,
                    "table_code": table_code},
            desc="user_substation_table",
        )
