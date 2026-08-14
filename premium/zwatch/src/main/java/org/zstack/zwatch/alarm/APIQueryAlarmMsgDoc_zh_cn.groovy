package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIQueryAlarmReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAlarm"

	category "zwatch.alarm"

	desc """查询报警器"""

	rest {
		request {
			url "GET /v1/zwatch/alarms"
			url "GET /v1/zwatch/alarms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAlarmMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAlarmReply.class
		}
	}
}