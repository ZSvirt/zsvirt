package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.APIQuerySNSWeComEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSWeComEndpoint"

	category "sns"

	desc """查询SNS企业微信终端"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/we-com"
			url "GET /v1/sns/application-endpoints/we-com/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSWeComEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSWeComEndpointReply.class
		}
	}
}