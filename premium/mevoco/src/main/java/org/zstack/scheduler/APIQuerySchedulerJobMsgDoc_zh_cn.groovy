package org.zstack.scheduler

import org.zstack.scheduler.APIQuerySchedulerJobReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySchedulerJob"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/scheduler/jobs"
			url "GET /v1/scheduler/jobs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySchedulerJobMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySchedulerJobReply.class
		}
	}
}