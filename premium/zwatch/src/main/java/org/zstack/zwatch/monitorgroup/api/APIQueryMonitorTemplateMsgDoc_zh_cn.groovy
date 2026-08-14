package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorTemplate"

	category "zwatch"

	desc """查询监控模板列表"""

	rest {
		request {
			url "GET /v1/zwatch/monitortemplates"
			url "GET /v1/zwatch/monitortemplates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorTemplateReply.class
		}
	}
}