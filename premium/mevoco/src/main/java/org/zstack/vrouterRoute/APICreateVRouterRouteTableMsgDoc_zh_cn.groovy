package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APICreateVRouterRouteTableEvent

doc {
	title "创建云路由路由表"

	category "vrouterRoute"

	desc """创建云路由路由表"""

	rest {
		request {
			url "POST /v1/vrouter-route-tables"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVRouterRouteTableMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateVRouterRouteTableEvent.class
		}
	}
}