package org.zstack.monitoring.actions

import org.zstack.monitoring.actions.APIQueryMonitorTriggerActionReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryEmailTriggerAction"

	category "monitoring"

	desc """查询Email报警动作"""

	rest {
		request {
			url "GET /v1/monitoring/trigger-actions/emails"
			url "GET /v1/monitoring/trigger-actions/emails/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEmailTriggerActionMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorTriggerActionReply.class
		}
	}
}