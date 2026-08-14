package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIGetVRouterRouteTableReply

doc {
	title "获取路由器实时路由表"

	category "vrouterRoute"

	desc """返回云路由设备实际实时路由表，包括非用户添加的全部路由"""

	rest {
		request {
			url "GET /v1/vrouter-route-tables/vrouter/{virtualRouterVmUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVRouterRouteTableMsg.class

			desc """"""

			params {

				column {
					name "virtualRouterVmUuid"
					enclosedIn ""
					desc "云路由设备UUID"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.1"
				}
			}
		}

		response {
			clz APIGetVRouterRouteTableReply.class
		}
	}
}