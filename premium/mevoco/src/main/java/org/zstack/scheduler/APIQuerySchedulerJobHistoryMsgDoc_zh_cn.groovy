package org.zstack.scheduler

import org.zstack.scheduler.APIQuerySchedulerJobHistoryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySchedulerJobHistory"

	category "scheduler"

	desc """查询定时任务记录"""

	rest {
		request {
			url "GET /v1/scheduler/job/history"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySchedulerJobHistoryMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySchedulerJobHistoryReply.class
		}
	}
}