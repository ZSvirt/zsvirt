package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIRestartResourceStackEvent

doc {
	title "RestartResourceStack"

	category "cloudformation"

	desc """重新启动一个资源编排堆栈"""

	rest {
		request {
			url "PUT /v1/cloudformation/stack/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRestartResourceStackMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "restartResourceStack"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.5.0"
				}
			}
		}

		response {
			clz APIRestartResourceStackEvent.class
		}
	}
}