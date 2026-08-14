package org.zstack.header.host

import org.zstack.header.host.APIQueryHostReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHost"

	category "host"

	desc """查询物理机"""

	rest {
		request {
			url "GET /v1/hosts"
			url "GET /v1/hosts/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostMsg.class

			desc """获取物理机清单"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostReply.class
		}
	}
}