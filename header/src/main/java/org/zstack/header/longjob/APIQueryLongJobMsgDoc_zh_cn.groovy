package org.zstack.header.longjob

import org.zstack.header.longjob.APIQueryLongJobReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryLongJob"

	category "longjob"

	desc """查询长任务"""

	rest {
		request {
			url "GET /v1/longjobs"
			url "GET /v1/longjobs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLongJobMsg.class

			desc """查询长任务"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLongJobReply.class
		}
	}
}