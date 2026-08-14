package org.zstack.billing.table

import org.zstack.billing.table.APICreatePriceTableEvent

doc {
	title "CreatePriceTable"

	category "billing"

	desc """创建计费价目表"""

	rest {
		request {
			url "POST /v1/billings/price-tables"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreatePriceTableMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "价目表名称"
					location "body"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "价目表的详细描述"
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "prices"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "3.7"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "价目表UUID"
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
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
			clz APICreatePriceTableEvent.class
		}
	}
}