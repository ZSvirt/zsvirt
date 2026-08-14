package org.zstack.scheduler

import org.zstack.scheduler.APIDeleteSchedulerTriggerEvent

doc {
	title "DeleteSchedulerTrigger"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/scheduler/triggers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteSchedulerTriggerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
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
			clz APIDeleteSchedulerTriggerEvent.class
		}
	}
}