package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIQueryAlarmRecordReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAlarmRecord"

	category "zwatch"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/zwatch/alarm-records"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAlarmRecordMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAlarmRecordReply.class
		}
	}
}