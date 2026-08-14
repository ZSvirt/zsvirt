package org.zstack.billing.table

import org.zstack.billing.table.APIUpdatePriceTableEvent

doc {
	title "UpdatePriceTable"

	category "billing"

	desc """修改价目表"""

	rest {
		request {
			url "PUT /v1/billings/price-tables/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePriceTableMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePriceTable"
					desc "价目表UUID"
					location "url"
					type "String"
					optional false
					since "3.7"
				}
				column {
					name "name"
					enclosedIn "updatePriceTable"
					desc "价目表名称"
					location "body"
					type "String"
					optional true
					since "3.7"
				}
				column {
					name "description"
					enclosedIn "updatePriceTable"
					desc "价目表描述"
					location "body"
					type "String"
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
			clz APIUpdatePriceTableEvent.class
		}
	}
}