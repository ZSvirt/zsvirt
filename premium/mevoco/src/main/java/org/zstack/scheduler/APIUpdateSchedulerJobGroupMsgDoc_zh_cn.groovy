package org.zstack.scheduler

import org.zstack.scheduler.APIUpdateSchedulerJobGroupEvent

doc {
	title "UpdateSchedulerJobGroup"

	category "scheduler"

	desc """更新定时任务组"""

	rest {
		request {
			url "PUT /v1/scheduler/jobgroups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSchedulerJobGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSchedulerJobGroup"
					desc "定时任务组的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "name"
					enclosedIn "updateSchedulerJobGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "description"
					enclosedIn "updateSchedulerJobGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.4.0"
				}
				column {
					name "state"
					enclosedIn "updateSchedulerJobGroup"
					desc "状态"
					location "body"
					type "String"
					optional true
					since "3.4.0"
					values ("Enabled","Disabled")
				}
				column {
					name "parameters"
					enclosedIn "updateSchedulerJobGroup"
					desc "定时任务组参数"
					location "body"
					type "Map"
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
			clz APIUpdateSchedulerJobGroupEvent.class
		}
	}
}