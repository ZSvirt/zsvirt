package org.zstack.sns

import org.zstack.sns.APIQuerySNSTopicSubscriberReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSTopicSubscriber"

	category "sns"

	desc """查询SNS topic订阅"""

	rest {
		request {
			url "GET /v1/sns/topics/subscribers"
			url "GET /v1/sns/topics/subscribers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSTopicSubscriberMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSTopicSubscriberReply.class
		}
	}
}