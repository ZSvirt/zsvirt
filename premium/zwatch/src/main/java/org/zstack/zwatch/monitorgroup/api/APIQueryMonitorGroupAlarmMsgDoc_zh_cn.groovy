package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupAlarmReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMonitorGroupAlarm"

	category "zwatch"

	desc """查询资源分组报警器列表"""

	rest {
		request {
			url "GET /v1/zwatch/monitorgroups/alarms"
			url "GET /v1/zwatch/monitorgroups/alarms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMonitorGroupAlarmMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMonitorGroupAlarmReply.class
		}
	}
}