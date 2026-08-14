package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIDetachVRouterRouteTableFromVRouterEvent

doc {
	title "解绑云路由路由表"

	category "vrouterRoute"

	desc """将云路由路由表从云路由设备解绑"""

	rest {
		request {
			url "DELETE /v1/vrouter-route-tables/{routeTableUuid}/detach/{virtualRouterVmUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachVRouterRouteTableFromVRouterMsg.class

			desc """"""

			params {

				column {
					name "routeTableUuid"
					enclosedIn ""
					desc "云路由路由表UUID"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "virtualRouterVmUuid"
					enclosedIn ""
					desc "云路由表设备UUID"
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
			clz APIDetachVRouterRouteTableFromVRouterEvent.class
		}
	}
}