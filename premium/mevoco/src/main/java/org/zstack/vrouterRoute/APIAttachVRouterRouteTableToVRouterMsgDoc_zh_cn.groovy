package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIAttachVRouterRouteTableToVRouterEvent

doc {
	title "绑定云路由路由表到云路由设备"

	category "vrouterRoute"

	desc """绑定云路由路由表到云路由设备"""

	rest {
		request {
			url "POST /v1/vrouter-route-tables/{routeTableUuid}/attach"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachVRouterRouteTableToVRouterMsg.class

			desc """"""

			params {

				column {
					name "routeTableUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "virtualRouterVmUuid"
					enclosedIn "params"
					desc "云路由设备UUID"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.1"
				}
			}
		}

		response {
			clz APIAttachVRouterRouteTableToVRouterEvent.class
		}
	}
}