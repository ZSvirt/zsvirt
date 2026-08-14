package org.zstack.scheduler

import org.zstack.scheduler.APIRunSchedulerTriggerEvent

doc {
	title "RunSchedulerTrigger"

	category "scheduler"

	desc """触发定时器"""

	rest {
		request {
			url "PUT /v1/scheduler/triggers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRunSchedulerTriggerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "runSchedulerTrigger"
					desc "定时器UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "jobUuids"
					enclosedIn "runSchedulerTrigger"
					desc "可选触发的任务UUID"
					location "body"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APIRunSchedulerTriggerEvent.class
		}
	}
}