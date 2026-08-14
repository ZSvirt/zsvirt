package org.zstack.zwatch.alarm.activealarm.api

import org.zstack.zwatch.alarm.activealarm.api.APIQueryActiveAlarmTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryActiveAlarmTemplate"

	category "zwatch"

	desc """查询一键报警模板"""

	rest {
		request {
			url "GET /v1/zwatch/activealarms/templates"
			url "GET /v1/zwatch/activealarms/templates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryActiveAlarmTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryActiveAlarmTemplateReply.class
		}
	}
}