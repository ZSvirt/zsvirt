package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupTemplateRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorGroupTemplateRef"

	category "zwatch"

	desc """查询资源分组应用的监控模板"""

	rest {
		request {
			url "GET /v1/zwatch/monitorgroups/monitortemplates/refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorGroupTemplateRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorGroupTemplateRefReply.class
		}
	}
}