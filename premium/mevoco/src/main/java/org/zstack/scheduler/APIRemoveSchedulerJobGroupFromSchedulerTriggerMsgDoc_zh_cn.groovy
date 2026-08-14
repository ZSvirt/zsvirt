package org.zstack.scheduler

import org.zstack.scheduler.APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent

doc {
	title "RemoveSchedulerJobGroupFromSchedulerTrigger"

	category "scheduler"

	desc """定时任务组解绑触发器"""

	rest {
		request {
			url "DELETE /v1/scheduler/jobgroups/{schedulerJobGroupUuid}/scheduler/triggers/{schedulerTriggerUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveSchedulerJobGroupFromSchedulerTriggerMsg.class

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
					name "schedulerTriggerUuid"
					enclosedIn ""
					desc "触发器UUID"
					location "url"
					type "String"
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
			clz APIRemoveSchedulerJobGroupFromSchedulerTriggerEvent.class
		}
	}
}