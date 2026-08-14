package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupEventSubscriptionReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorGroupEventSubscription"

	category "zwatch"

	desc """查询资源分组事件报警器列表"""

	rest {
		request {
			url "GET /v1/zwatch/monitorgroups/subscriptions"
			url "GET /v1/zwatch/monitorgroups/subscriptions/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorGroupEventSubscriptionMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorGroupEventSubscriptionReply.class
		}
	}
}