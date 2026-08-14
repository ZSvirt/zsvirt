package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APIDeleteEmailAddressOfSNSEmailEndpointEvent

doc {
	title "DeleteEmailAddressOfSNSEmailEndpoint"

	category "sns"

	desc """删除邮箱接收端的地址"""

	rest {
		request {
			url "DELETE /v1/sns/application-endpoints/emails/{endpointUuid}/email-addresses/{emailAddressUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteEmailAddressOfSNSEmailEndpointMsg.class

			desc """"""

			params {

				column {
					name "emailAddressUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "endpointUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIDeleteEmailAddressOfSNSEmailEndpointEvent.class
		}
	}
}