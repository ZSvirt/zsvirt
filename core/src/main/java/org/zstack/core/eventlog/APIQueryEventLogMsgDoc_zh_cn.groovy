package org.zstack.core.eventlog

import org.zstack.core.eventlog.APIQueryEventLogReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryEventLog"

	category "未知类别"

	desc """查询Event Log"""

	rest {
		request {
			url "GET /v1/eventlogs"
			url "GET /v1/eventlogs/{id}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEventLogMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryEventLogReply.class
		}
	}
}