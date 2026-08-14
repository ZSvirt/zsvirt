package org.zstack.billing.table

import org.zstack.billing.table.APIGetAccountPriceTableRefReply

doc {
	title "GetAccountPriceTableRef"

	category "billing"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/billings/price-tables/refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAccountPriceTableRefMsg.class

			desc """"""

			params {

				column {
					name "tableUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "accountUuid"
					enclosedIn ""
					desc "账户UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetAccountPriceTableRefReply.class
		}
	}
}