package org.zstack.sns

import org.zstack.sns.APIQuerySNSSmsEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSSmsEndpoint"

	category "sns"

	desc """查询短信接收端"""

	rest {
		request {
			url "GET /v1/sns/sms-endpoints"
			url "GET /v1/sns/sms-endpoints/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSSmsEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSSmsEndpointReply.class
		}
	}
}