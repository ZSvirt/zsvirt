package org.zstack.header.identity

import org.zstack.header.identity.APIQueryQuotaReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryQuota"

	category "identity"

	desc """查询配额"""

	rest {
		request {
			url "GET /v1/accounts/quotas"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryQuotaMsg.class

			desc """查询配额"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryQuotaReply.class
		}
	}
}