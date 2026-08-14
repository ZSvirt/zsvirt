package org.zstack.sns.platform.dingtalk

import org.zstack.sns.platform.dingtalk.APIQuerySNSDingTalkAtPersonReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSDingTalkAtPerson"

	category "sns"

	desc """查询钉钉@用户"""

	rest {
		request {
			url "GET /v1/sns/application-endpoints/ding-talk/at-persons"
			url "GET /v1/sns/application-endpoints/ding-talk/at-persons/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSDingTalkAtPersonMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSDingTalkAtPersonReply.class
		}
	}
}