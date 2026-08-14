package org.zstack.billing.table

import org.zstack.billing.table.APIQueryPriceTableRely
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryPriceTable"

	category "billing"

	desc """查询计费价目表"""

	rest {
		request {
			url "GET /v1/billings/price-tables"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPriceTableMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPriceTableRely.class
		}
	}
}