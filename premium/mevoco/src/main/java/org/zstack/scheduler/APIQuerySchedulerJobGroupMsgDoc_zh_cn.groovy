package org.zstack.scheduler

import org.zstack.scheduler.APIQuerySchedulerJobGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySchedulerJobGroup"

	category "scheduler"

	desc """查询定时任务组"""

	rest {
		request {
			url "GET /v1/scheduler/jobgroups"
			url "GET /v1/scheduler/jobgroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySchedulerJobGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySchedulerJobGroupReply.class
		}
	}
}