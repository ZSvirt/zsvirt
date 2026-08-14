package org.zstack.billing.table

import org.zstack.billing.table.APIAttachPriceTableToAccountEvent

doc {
	title "AttachPriceTableToAccount"

	category "billing"

	desc """给账号指定计费价目"""

	rest {
		request {
			url "POST /v1/billings/price-tables/{tableUuid}/accounts/{accountUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachPriceTableToAccountMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "tableUuid"
					enclosedIn "params"
					desc "价目表UUID"
					location "url"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "tagUuids"
					enclosedIn ""
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.7"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7"
				}
			}
		}

		response {
			clz APIAttachPriceTableToAccountEvent.class
		}
	}
}