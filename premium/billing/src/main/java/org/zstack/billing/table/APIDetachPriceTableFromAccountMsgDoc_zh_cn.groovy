package org.zstack.billing.table

import org.zstack.billing.table.APIDetachPriceTableFromAccountEvent

doc {
	title "DetachPriceTableFromAccount"

	category "billing"

	desc """取消账号关联的计费价目"""

	rest {
		request {
			url "DELETE /v1/billings/price-tables/{tableUuid}/accounts/{accountUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachPriceTableFromAccountMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn ""
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "tableUuid"
					enclosedIn ""
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
					location "query"
					type "List"
					optional true
					since "3.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.7"
				}
			}
		}

		response {
			clz APIDetachPriceTableFromAccountEvent.class
		}
	}
}