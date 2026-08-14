package org.zstack.header.host

import org.zstack.header.host.APIReconnectHostEvent

doc {
	title "ReconnectHost"

	category "host"

	desc """重连物理机"""

	rest {
		request {
			url "PUT /v1/hosts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReconnectHostMsg.class

			desc """重新连接物理机"""

			params {

				column {
					name "uuid"
					enclosedIn "reconnectHost"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
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
			clz APIReconnectHostEvent.class
		}
	}
}