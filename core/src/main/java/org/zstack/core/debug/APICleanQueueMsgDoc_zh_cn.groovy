package org.zstack.core.debug

import org.zstack.core.debug.APICleanQueueEvent

doc {
	title "CleanQueue"

	category "debug"

	desc """清理管理节点队列"""

	rest {
		request {
			url "PUT /v1/clean/queue"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanQueueMsg.class

			desc """"""

			params {

				column {
					name "signatureName"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.11.3"
				}
				column {
					name "taskIndex"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.11.3"
				}
				column {
					name "isCleanUp"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "3.11.3"
				}
				column {
					name "isRunningTask"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "3.11.3"
				}
				column {
					name "managementiUuid"
					enclosedIn "cleanQueue"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.11.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.11.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.11.3"
				}
			}
		}

		response {
			clz APICleanQueueEvent.class
		}
	}
}