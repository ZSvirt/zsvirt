package org.zstack.monitoring

import org.zstack.monitoring.APIQueryMonitorTriggerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorTrigger"

	category "monitoring"

	desc """查询报警器"""

	rest {
		request {
			url "GET /v1/monitoring/triggers"
			url "GET /v1/monitoring/triggers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorTriggerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorTriggerReply.class
		}
	}
}