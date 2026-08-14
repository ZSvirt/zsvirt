package org.zstack.billing.table

import org.zstack.billing.table.APIDeletePriceTableEvent

doc {
	title "DeletePriceTable"

	category "billing"

	desc """删除价目表"""

	rest {
		request {
			url "DELETE /v1/billings/price-tables/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeletePriceTableMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "价目表UUID"
					location "url"
					type "String"
					optional false
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
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeletePriceTableEvent.class
		}
	}
}