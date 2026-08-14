package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIProvisionVirtualRouterConfigEvent

doc {
	title "重新配置虚拟路由器(ProvisionVirtualRouterConfig)"

	category "虚拟路由器"

	desc """重新配置虚拟路由器"""

	rest {
		request {
			url "PUT /v1/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/provision"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIProvisionVirtualRouterConfigMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "provisionVirtualRouterConfig"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
			}
		}

		response {
			clz APIProvisionVirtualRouterConfigEvent.class
		}
	}
}