package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APIAddEmailAddressToSNSEmailEndpointEvent

doc {
	title "AddEmailAddressToSNSEmailEndpoint"

	category "sns"

	desc """给SNS邮件接收端增加邮箱地址"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/emails/email-addresses"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddEmailAddressToSNSEmailEndpointMsg.class

			desc """"""

			params {

				column {
					name "emailAddress"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
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
			clz APIAddEmailAddressToSNSEmailEndpointEvent.class
		}
	}
}