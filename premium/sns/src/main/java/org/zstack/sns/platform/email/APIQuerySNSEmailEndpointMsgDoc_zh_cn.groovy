package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APIQuerySNSEmailEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSEmailEndpoint"

	category "sns"

	desc """查询SNS邮件终端"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/emails"
			url "GET /v1/sns/application-endpoints/emails/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSEmailEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSEmailEndpointReply.class
		}
	}
}