package org.zstack.header.baremetal.pxeserver

import org.zstack.header.baremetal.pxeserver.APIReconnectBaremetalPxeServerEvent

doc {
	title "ReconnectBaremetalPxeServer"

	category "baremetal.pxeserver"

	desc """重连部署服务器返回"""

	rest {
		request {
			url "PUT /v1/baremetal/pxeservers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReconnectBaremetalPxeServerMsg.class

			desc """重连部署服务器返回"""

			params {

				column {
					name "uuid"
					enclosedIn "reconnectBaremetalPxeServer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.1.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.1.1"
				}
			}
		}

		response {
			clz APIReconnectBaremetalPxeServerEvent.class
		}
	}
}