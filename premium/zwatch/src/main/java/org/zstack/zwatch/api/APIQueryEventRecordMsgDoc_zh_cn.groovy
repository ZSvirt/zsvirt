package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIQueryEventRecordReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryEventRecord"

	category "zwatch"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/zwatch/event-records"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEventRecordMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryEventRecordReply.class
		}
	}
}