package org.zstack.scheduler

import org.zstack.scheduler.APIChangeSchedulerStateEvent

doc {
	title "ChangeSchedulerState"

	category "core.scheduler"

	desc """改变定时任务状态"""

	rest {
		request {
			url "PUT /v1/schedulers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeSchedulerStateMsg.class

			desc """改变定时任务状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changeSchedulerState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeSchedulerState"
					desc "要设置的定时任务状态"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIChangeSchedulerStateEvent.class
		}
	}
}