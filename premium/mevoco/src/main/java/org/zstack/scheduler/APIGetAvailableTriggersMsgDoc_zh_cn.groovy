package org.zstack.scheduler

import org.zstack.scheduler.APIGetAvailableTriggersReply

doc {
	title "GetAvailableTriggers"

	category "scheduler"

	desc """获取可用的定时器"""

	rest {
		request {
			url "GET /v1/scheduler/triggers/available"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAvailableTriggersMsg.class

			desc """"""

			params {

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
			clz APIGetAvailableTriggersReply.class
		}
	}
}