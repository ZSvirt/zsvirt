package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIReconnectVirtualRouterEvent

doc {
	title "重连虚拟路由器(ReconnectVirtualRouter)"

	category "虚拟路由器"

	desc """重连虚拟路由器"""

	rest {
		request {
			url "PUT /v1/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReconnectVirtualRouterMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "reconnectVirtualRouter"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
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
			clz APIReconnectVirtualRouterEvent.class
		}
	}
}