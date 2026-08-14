package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIDeleteVRouterRouteTableEvent

doc {
	title "删除云路由路由表"

	category "vrouterRoute"

	desc """删除云路由路由表"""

	rest {
		request {
			url "DELETE /v1/vrouter-route-tables/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVRouterRouteTableMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
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
			clz APIDeleteVRouterRouteTableEvent.class
		}
	}
}