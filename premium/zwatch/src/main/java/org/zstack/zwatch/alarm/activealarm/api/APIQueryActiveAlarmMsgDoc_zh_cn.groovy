package org.zstack.zwatch.alarm.activealarm.api

import org.zstack.zwatch.alarm.activealarm.api.APIQueryActiveAlarmReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryActiveAlarm"

	category "zwatch"

	desc """查询一键报警的报警器列表"""

	rest {
		request {
			url "GET /v1/zwatch/activealarms/alarms"
			url "GET /v1/zwatch/activealarms/alarms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryActiveAlarmMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryActiveAlarmReply.class
		}
	}
}