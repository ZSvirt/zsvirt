package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIUpdateVirtualRouterEvent

doc {
	title "更新虚拟路由器(UpdateVirtualRouter)"

	category "虚拟路由器"

	desc """更新虚拟路由器"""

	rest {
		request {
			url "PUT /v1/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVirtualRouterMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "updateVirtualRouter"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.8"
				}
				column {
					name "defaultRouteL3NetworkUuid"
					enclosedIn "updateVirtualRouter"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.8"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.8"
				}
			}
		}

		response {
			clz APIUpdateVirtualRouterEvent.class
		}
	}
}