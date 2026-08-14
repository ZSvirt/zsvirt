package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIQueryAlertDataAckReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAlertDataAck"

	category "zwatch"

	desc """查询报警确认信息列表"""

	rest {
		request {
			url "GET /v1/zwatch/alert-histories/acknowledgments"
			url "GET /v1/zwatch/alert-histories/acknowledgments/{alertDataUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAlertDataAckMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAlertDataAckReply.class
		}
	}
}