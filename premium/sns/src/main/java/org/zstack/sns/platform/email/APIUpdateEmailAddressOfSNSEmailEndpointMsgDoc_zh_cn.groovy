package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APIUpdateEmailAddressOfSNSEmailEndpointEvent

doc {
	title "UpdateEmailAddressOfSNSEmailEndpoint"

	category "sns"

	desc """更新接收端邮箱地址"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/emails/email-addresses"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateEmailAddressOfSNSEmailEndpointMsg.class

			desc """"""

			params {

				column {
					name "emailAddressUuid"
					enclosedIn "updateEmailAddressOfSNSEmailEndpoint"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "updateEmailAddressOfSNSEmailEndpoint"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "emailAddress"
					enclosedIn "updateEmailAddressOfSNSEmailEndpoint"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIUpdateEmailAddressOfSNSEmailEndpointEvent.class
		}
	}
}