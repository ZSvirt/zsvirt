package org.zstack.billing.userconfig

import org.zstack.billing.userconfig.APIValidateInstanceOfferingUserConfigEvent

doc {
	title "ValidateInstanceOfferingUserConfig"

	category "billing"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/billings/accounts/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIValidateInstanceOfferingUserConfigMsg.class

			desc """"""

			params {

				column {
					name "config"
					enclosedIn "validateInstanceOfferingUserConfig"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIValidateInstanceOfferingUserConfigEvent.class
		}
	}
}