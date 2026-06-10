package org.zstack.zwatch.resnotify

import org.zstack.zwatch.resnotify.APIQueryResNotifySubscriptionReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询资源变更通知订阅"

	category "zwatch"

	desc """查询资源变更通知订阅，可按 UUID 或查询条件获取订阅配置、状态以及 webhook 引用信息。"""

	rest {
		request {
			url "GET /v1/zwatch/resnotify/subscriptions"
			url "GET /v1/zwatch/resnotify/subscriptions/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryResNotifySubscriptionMsg.class

			desc """查询资源变更通知订阅请求。"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryResNotifySubscriptionReply.class
		}
	}
}