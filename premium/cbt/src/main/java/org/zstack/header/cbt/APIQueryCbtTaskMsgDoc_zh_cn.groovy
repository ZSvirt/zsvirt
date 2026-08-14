package org.zstack.header.cbt

import org.zstack.header.cbt.APIQueryCbtTaskReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryCbtTask"

	category "cbt"

	desc """查询CBT任务"""

	rest {
		request {
			url "GET /v1/cbt-task"
			url "GET /v1/cbt-task/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryCbtTaskMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryCbtTaskReply.class
		}
	}
}