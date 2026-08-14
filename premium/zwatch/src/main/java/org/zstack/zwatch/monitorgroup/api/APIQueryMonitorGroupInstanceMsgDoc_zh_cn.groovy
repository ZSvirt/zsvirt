package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupInstanceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorGroupInstance"

	category "zwatch"

	desc """查询资源分组资源列表"""

	rest {
		request {
			url "GET /v1/zwatch/monitorgroups/instances"
			url "GET /v1/zwatch/monitorgroups/instances/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorGroupInstanceMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorGroupInstanceReply.class
		}
	}
}