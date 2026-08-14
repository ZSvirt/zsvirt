package org.zstack.sns.platform.dingtalk

import org.zstack.sns.platform.dingtalk.APIQuerySNSDingTalkEndpointReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSDingTalkEndpoint"

	category "sns"

	desc """查询钉钉终端"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/ding-talk"
			url "GET /v1/sns/application-endpoints/ding-talk/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSDingTalkEndpointMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSDingTalkEndpointReply.class
		}
	}
}