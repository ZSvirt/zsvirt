package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.APIQuerySNSFeiShuEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSFeiShuEndpoint"

	category "sns"

	desc """查询SNS飞书终端"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/feishu"
			url "GET /v1/sns/application-endpoints/feishu/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSFeiShuEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSFeiShuEndpointReply.class
		}
	}
}