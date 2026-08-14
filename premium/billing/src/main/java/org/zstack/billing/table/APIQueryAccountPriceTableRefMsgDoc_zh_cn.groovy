package org.zstack.billing.table

import org.zstack.billing.table.APIQueryAccountPriceTableRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccountPriceTableRef"

	category "billing"

	desc """查询账号价目表关联关系"""

	rest {
		request {
			url "GET /v1/accounts/price-tables/refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccountPriceTableRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccountPriceTableRefReply.class
		}
	}
}