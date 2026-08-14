package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorGroup"

	category "zwatch"

	desc """查询资源分组列表"""

	rest {
		request {
			url "GET /v1/zwatch/monitorgroups"
			url "GET /v1/zwatch/monitorgroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorGroupReply.class
		}
	}
}