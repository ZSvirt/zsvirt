package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIQueryEventSubscriptionReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryEventSubscription"

	category "zwatch.alarm"

	desc """查询订阅事件"""

	rest {
		request {
			url "GET /v1/zwatch/events/subscriptions"
			url "GET /v1/zwatch/events/subscriptions/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEventSubscriptionMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryEventSubscriptionReply.class
		}
	}
}