package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIUpdateVRouterRouteTableEvent

doc {
	title "UpdateVRouterRouteTable"

	category "vrouterRoute"

	desc """更新云路由路由表"""

	rest {
		request {
			url "PUT /v1/vrouter-route-tables/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVRouterRouteTableMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVRouterRouteTable"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "name"
					enclosedIn "updateVRouterRouteTable"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.3.1"
				}
				column {
					name "description"
					enclosedIn "updateVRouterRouteTable"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3.1"
				}
			}
		}

		response {
			clz APIUpdateVRouterRouteTableEvent.class
		}
	}
}