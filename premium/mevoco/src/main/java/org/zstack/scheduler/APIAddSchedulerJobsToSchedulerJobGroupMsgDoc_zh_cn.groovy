package org.zstack.scheduler

import org.zstack.scheduler.APIAddSchedulerJobsToSchedulerJobGroupEvent

doc {
	title "AddSchedulerJobsToSchedulerJobGroup"

	category "scheduler"

	desc """任务组添加任务"""

	rest {
		request {
			url "POST /v1/scheduler/jobgroups/{schedulerJobGroupUuid}/job"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSchedulerJobsToSchedulerJobGroupMsg.class

			desc """"""

			params {

				column {
					name "schedulerJobGroupUuid"
					enclosedIn "params"
					desc "定时任务组UUID"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "schedulerJobUuids"
					enclosedIn "params"
					desc "定时任务UUID列表"
					location "body"
					type "List"
					optional false
					since "3.4.0"
				}
				column {
					name "priorities"
					enclosedIn "params"
					desc "定时任务优先级"
					location "body"
					type "Map"
					optional true
					since "zsv 4.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddSchedulerJobsToSchedulerJobGroupEvent.class
		}
	}
}