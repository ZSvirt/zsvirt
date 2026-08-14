package org.zstack.sns

import org.zstack.sns.APIQuerySNSApplicationEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSApplicationEndpoint"

	category "sns"

	desc """查询SNS应用终端"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints"
			url "GET /v1/sns/application-endpoints/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSApplicationEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSApplicationEndpointReply.class
		}
	}
}