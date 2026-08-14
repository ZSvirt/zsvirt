package org.zstack.header.host

import org.zstack.header.host.APIChangeHostStateEvent

doc {
	title "ChangeHostState"

	category "host"

	desc """更新物理机启用状态"""

	rest {
		request {
			url "PUT /v1/hosts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeHostStateMsg.class

			desc """更新物理机启用状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changeHostState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeHostState"
					desc "期望的物理机启用状态"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					values ("enable","disable","maintain")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIChangeHostStateEvent.class
		}
	}
}