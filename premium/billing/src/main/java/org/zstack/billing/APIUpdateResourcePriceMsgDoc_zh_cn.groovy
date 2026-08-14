package org.zstack.billing

import org.zstack.billing.APIUpdateResourcePriceEvent

doc {
	title "UpdateResourcePrice"

	category "billing"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/billings/prices/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateResourcePriceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateResourcePrice"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "endDateInLong"
					enclosedIn "updateResourcePrice"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "setEndDateInLongBaseOnCurrentTime"
					enclosedIn "updateResourcePrice"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateResourcePriceEvent.class
		}
	}
}