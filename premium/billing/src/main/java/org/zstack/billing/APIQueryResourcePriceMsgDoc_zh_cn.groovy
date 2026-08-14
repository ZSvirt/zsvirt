package org.zstack.billing

import org.zstack.billing.APIQueryResourcePriceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryResourcePrice"

	category "billing"

	desc """查询资源价格"""

	rest {
		request {
			url "GET /v1/billings/prices"
			url "GET /v1/billing/prices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryResourcePriceMsg.class

			desc """查询资源价格"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryResourcePriceReply.class
		}
	}
}