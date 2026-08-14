package org.zstack.scheduler

import org.zstack.scheduler.APIRemoveSchedulerJobFromSchedulerTriggerEvent

doc {
	title "RemoveSchedulerJobFromSchedulerTrigger"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/scheduler/jobs/{schedulerJobUuid}/scheduler/triggers/{schedulerTriggerUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveSchedulerJobFromSchedulerTriggerMsg.class

			desc """"""

			params {

				column {
					name "schedulerJobUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "schedulerTriggerUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
				}
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
			clz APIRemoveSchedulerJobFromSchedulerTriggerEvent.class
		}
	}
}