package org.zstack.scheduler

import org.zstack.scheduler.APICreateSchedulerJobEvent

doc {
	title "CreateSchedulerJob"

	category "scheduler"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/scheduler/jobs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSchedulerJobMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "targetResourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("startVm","stopVm","rebootVm","volumeSnapshot")
				}
				column {
					name "parameters"
					enclosedIn "params"
					desc ""
					location "body"
					type "Map"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
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
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateSchedulerJobEvent.class
		}
	}
}