package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIQueryMetricDataHttpReceiverReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMetricTemplate"

	category "zwatch"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/zwatch/metrics/httpreceivers/templates"
			url "GET /v1/zwatch/metrics/httpreceivers/templates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMetricTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMetricDataHttpReceiverReply.class
		}
	}
}