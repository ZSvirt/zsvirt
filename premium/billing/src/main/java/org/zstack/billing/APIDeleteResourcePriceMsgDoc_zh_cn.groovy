package org.zstack.billing

import org.zstack.billing.APIDeleteResourcePriceEvent

doc {
	title "DeleteResourcePrice"

	category "billing"

	desc """删除资源价格"""

	rest {
		request {
			url "DELETE /v1/billings/prices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteResourcePriceMsg.class

			desc """删除资源价格"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
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
				column {
					name "cutoffPrice"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteResourcePriceEvent.class
		}
	}
}