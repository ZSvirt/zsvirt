package org.zstack.scheduler

import org.zstack.scheduler.APIRemoveSchedulerJobsFromSchedulerJobGroupEvent

doc {
	title "RemoveSchedulerJobsFromSchedulerJobGroup"

	category "scheduler"

	desc """定时任务组移除定时任务"""

	rest {
		request {
			url "DELETE /v1/scheduler/jobgroups/{schedulerJobGroupUuid}/job"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveSchedulerJobsFromSchedulerJobGroupMsg.class

			desc """"""

			params {

				column {
					name "schedulerJobGroupUuid"
					enclosedIn ""
					desc "定时任务组UUID"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "schedulerJobUuids"
					enclosedIn ""
					desc "定时任务i列表"
					location "query"
					type "List"
					optional false
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签列表"
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签列表"
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIRemoveSchedulerJobsFromSchedulerJobGroupEvent.class
		}
	}
}