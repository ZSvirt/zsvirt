package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIAddVRouterRouteEntryEvent

doc {
	title "添加云路由路由表条目"

	category "vrouterRoute"

	desc """向云路由路由表添加路由条目"""

	rest {
		request {
			url "POST /v1/vrouter-route-tables/{routeTableUuid}/route-entries"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddVRouterRouteEntryMsg.class

			desc """"""

			params {

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
					name "type"
					enclosedIn "params"
					desc "类型，允许用户添加‘静态路由’、‘黑洞路由’两种类型，系统会根据是否填下一条自动判断类型"
					location "body"
					type "String"
					optional true
					since "2.1"
					values ("UserStatic","UserBlackHole")
				}
				column {
					name "routeTableUuid"
					enclosedIn "params"
					desc "云路由路由表UUID"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "destination"
					enclosedIn "params"
					desc "目标网络地址，使用网络地址CIDR格式，如果用户填写的不是标准CIDR格式，系统会自动转换"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "target"
					enclosedIn "params"
					desc "下一条地址，为一个云路由设备目前可以直接到达的IP地址，如果不可以直接到达，将会进行递归路由"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "distance"
					enclosedIn "params"
					desc "路由优先级，在最小匹配下如果有多条路由规则匹配，优先级数字小的规则将会被匹配"
					location "body"
					type "Integer"
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
			clz APIAddVRouterRouteEntryEvent.class
		}
	}
}