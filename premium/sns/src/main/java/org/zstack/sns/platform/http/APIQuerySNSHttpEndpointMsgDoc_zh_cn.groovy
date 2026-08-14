package org.zstack.sns.platform.http

import org.zstack.sns.platform.http.APIQuerySNSHttpEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSHttpEndpoint"

	category "sns"

	desc """查询SNS HTTP终端"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/http"
			url "GET /v1/sns/application-endpoints/http/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSHttpEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSHttpEndpointReply.class
		}
	}
}