package org.zstack.scheduler

import org.zstack.scheduler.APIAddSchedulerJobGroupToSchedulerTriggerEvent

doc {
	title "AddSchedulerJobGroupToSchedulerTrigger"

	category "scheduler"

	desc """定时任务组绑定触发器结果"""

	rest {
		request {
			url "POST /v1/scheduler/jobgroups/{schedulerJobGroupUuid}/scheduler/triggers/{schedulerTriggerUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSchedulerJobGroupToSchedulerTriggerMsg.class

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
					name "schedulerTriggerUuid"
					enclosedIn "params"
					desc "触发器UUID"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "triggerNow"
					enclosedIn "params"
					desc "是否立即触发"
					location "body"
					type "boolean"
					optional true
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddSchedulerJobGroupToSchedulerTriggerEvent.class
		}
	}
}