package org.zstack.sns

import org.zstack.sns.APIQuerySNSTopicReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSTopic"

	category "sns"

	desc """查询SNS主题"""

	rest {
		request {
			url "GET /v1/sns/topics"
			url "GET /v1/sns/topics/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSTopicMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSTopicReply.class
		}
	}
}