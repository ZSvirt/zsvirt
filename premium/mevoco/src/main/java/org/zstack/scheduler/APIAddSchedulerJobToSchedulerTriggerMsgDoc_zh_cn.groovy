package org.zstack.scheduler

import org.zstack.scheduler.APIAddSchedulerJobToSchedulerTriggerEvent

doc {
	title "AddSchedulerJobToSchedulerTrigger"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/scheduler/jobs/{schedulerJobUuid}/scheduler/triggers/{schedulerTriggerUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSchedulerJobToSchedulerTriggerMsg.class

			desc """"""

			params {

				column {
					name "schedulerJobUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "schedulerTriggerUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "triggerNow"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "3.2.0"
				}
			}
		}

		response {
			clz APIAddSchedulerJobToSchedulerTriggerEvent.class
		}
	}
}