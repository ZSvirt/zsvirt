package org.zstack.monitoring.actions

import org.zstack.monitoring.actions.APIQueryMonitorTriggerActionReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorTriggerAction"

	category "monitoring"

	desc """查询报警器动作"""

	rest {
		request {
			url "GET /v1/monitoring/trigger-actions"
			url "GET /v1/monitoring/trigger-actions/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorTriggerActionMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorTriggerActionReply.class
		}
	}
}