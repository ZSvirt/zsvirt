package org.zstack.billing

import org.zstack.billing.APIQueryAccountBillingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccountBilling"

	category "billing"

	desc """查询账户账单"""

	rest {
		request {
			url "GET /v1/billing/billings"
			url "GET /v1/billing/billings/{id}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccountBillingMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccountBillingReply.class
		}
	}
}