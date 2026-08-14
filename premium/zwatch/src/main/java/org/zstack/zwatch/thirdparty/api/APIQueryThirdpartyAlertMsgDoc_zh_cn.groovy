package org.zstack.zwatch.thirdparty.api

import org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyAlertReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryThirdpartyAlert"

	category "zwatch"

	desc """查询第三方报警消息"""

	rest {
		request {
			url "GET /v1/zwatch/third-party/alerts"
			url "GET /v1/zwatch/third-party/alerts/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryThirdpartyAlertMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryThirdpartyAlertReply.class
		}
	}
}