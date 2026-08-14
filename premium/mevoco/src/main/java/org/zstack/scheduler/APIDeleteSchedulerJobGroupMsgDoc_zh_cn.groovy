package org.zstack.scheduler

import org.zstack.scheduler.APIDeleteSchedulerJobGroupEvent

doc {
	title "DeleteSchedulerJobGroup"

	category "scheduler"

	desc """删除定时任务组"""

	rest {
		request {
			url "DELETE /v1/scheduler/jobgroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteSchedulerJobGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "定时任务组的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
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
			clz APIDeleteSchedulerJobGroupEvent.class
		}
	}
}