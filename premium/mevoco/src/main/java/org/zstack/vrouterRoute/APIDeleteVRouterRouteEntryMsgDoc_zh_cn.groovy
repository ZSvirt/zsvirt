package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIDeleteVRouterRouteEntryEvent

doc {
	title "删除路由条目"

	category "vrouterRoute"

	desc """删除云路由路由表条目"""

	rest {
		request {
			url "DELETE /v1/vrouter-route-tables/{routeTableUuid}/route-entries/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVRouterRouteEntryMsg.class

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
					name "routeTableUuid"
					enclosedIn ""
					desc ""
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
			clz APIDeleteVRouterRouteEntryEvent.class
		}
	}
}