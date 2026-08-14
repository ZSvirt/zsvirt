package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIGetAttachablePublicL3ForVRouterReply

doc {
	title "获取云路由可加载外部网络(GetAttachablePublicL3ForVRouter)"

	category "云路由"

	desc """获取云路由可加载的公网和系统网络，将自动排除到地址冲突的网络"""

	rest {
		request {
			url "GET /v1/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/attachable-public-l3s"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAttachablePublicL3ForVRouterMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云路由的UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIGetAttachablePublicL3ForVRouterReply.class
		}
	}
}