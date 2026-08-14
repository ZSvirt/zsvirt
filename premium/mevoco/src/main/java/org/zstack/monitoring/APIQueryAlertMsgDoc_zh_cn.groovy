package org.zstack.monitoring

import org.zstack.monitoring.APIQueryAlertReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAlert"

	category "monitoring"

	desc """查询报警记录"""

	rest {
		request {
			url "GET /v1/monitoring/alerts"
			url "GET /v1/monitoring/alerts/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAlertMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAlertReply.class
		}
	}
}