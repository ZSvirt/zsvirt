package org.zstack.log.server

import org.zstack.log.server.APIQueryLogServerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryLogServer"

	category "logServer"

	desc """查询日志服务器"""

	rest {
		request {
			url "GET /v1/log/servers"
			url "GET /v1/log/servers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLogServerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLogServerReply.class
		}
	}
}