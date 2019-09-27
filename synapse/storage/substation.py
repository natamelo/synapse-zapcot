import logging

from synapse.storage._base import SQLBaseStore
from synapse.api.errors import StoreError
from twisted.internet import defer

logger = logging.getLogger(__name__)


class SubstationStore(SQLBaseStore):

    @defer.inlineCallbacks
    def get_substations(self):
        try:
            substations = yield self._simple_select_list(
                "substation",
                keyvalues=None,
                retcols=("code", "name", "company_code"),
            )

            return substations
        except Exception as e:
            logger.warning("get_substation failed: %s", e)
            raise StoreError(500, "Problem recovering substations")

    @defer.inlineCallbacks
    def get_substation_by_company_code_and_substation_code(self,
                                                           company_code, substation_code):
        substation = yield self._simple_select_one(
            table="substation",
            keyvalues={"company_code": company_code, "code": substation_code},
            retcols=("code", "name", "company_code"),
            allow_none=True,
            desc="get_substation_by_company_code_and_substation_code",
        )
        return substation
