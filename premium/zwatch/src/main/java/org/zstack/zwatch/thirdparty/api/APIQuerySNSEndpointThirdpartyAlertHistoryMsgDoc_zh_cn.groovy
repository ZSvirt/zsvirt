package org.zstack.zwatch.thirdparty.api

import org.zstack.zwatch.thirdparty.api.APIQuerySNSEndpointThirdpartyAlertHistoryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySNSEndpointThirdpartyAlertHistory"

	category "zwatch"

	desc """查询第三方报警推送历史"""

	rest {
		request {
			url "GET /v1/zwatch/third-party/alert-publish-histories"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySNSEndpointThirdpartyAlertHistoryMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySNSEndpointThirdpartyAlertHistoryReply.class
		}
	}
}