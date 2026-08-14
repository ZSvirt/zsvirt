package org.zstack.scheduler

import org.zstack.scheduler.APIGetNoTriggerSchedulerJobsReply

doc {
	title "GetNoTriggerSchedulerJobs"

	category "scheduler"

	desc """获取未挂载定时器的任务"""

	rest {
		request {
			url "GET /v1/scheduler/jobs/candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetNoTriggerSchedulerJobsMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetNoTriggerSchedulerJobsReply.class
		}
	}
}