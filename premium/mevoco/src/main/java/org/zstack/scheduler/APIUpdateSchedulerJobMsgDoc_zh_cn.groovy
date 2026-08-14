package org.zstack.scheduler

import org.zstack.scheduler.APIUpdateSchedulerJobEvent

doc {
	title "UpdateSchedulerJob"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/scheduler/jobs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSchedulerJobMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSchedulerJob"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateSchedulerJob"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateSchedulerJob"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
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
					name "parameters"
					enclosedIn "updateSchedulerJob"
					desc ""
					location "body"
					type "Map"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateSchedulerJobEvent.class
		}
	}
}