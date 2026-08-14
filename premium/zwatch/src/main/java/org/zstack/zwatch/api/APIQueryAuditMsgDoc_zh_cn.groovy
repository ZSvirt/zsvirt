package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIQueryAuditReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAudit"

	category "zwatch"

	desc """查询审计数据"""

	rest {
		request {
			url "GET /v1/zwatch/audit-records"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAuditMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAuditReply.class
		}
	}
}