package org.zstack.billing.table

import org.zstack.billing.table.APIChangeAccountPriceTableBindingEvent

doc {
	title "ChangeAccountPriceTableBinding"

	category "billing"

	desc """修改账号计费价目"""

	rest {
		request {
			url "PUT /v1/billings/price-tables/{tableUuid}/accounts/{accountUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAccountPriceTableBindingMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn "changeAccountPriceTableBinding"
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "tableUuid"
					enclosedIn "changeAccountPriceTableBinding"
					desc "价目表UUID"
					location "url"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "resourceUuid"
					enclosedIn "changeAccountPriceTableBinding"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "tagUuids"
					enclosedIn "changeAccountPriceTableBinding"
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
			clz APIChangeAccountPriceTableBindingEvent.class
		}
	}
}