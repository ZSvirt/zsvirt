package org.zstack.header.protocol

import org.zstack.header.protocol.APIGetVRouterRouterIdReply

doc {
	title "GetVRouterRouterId"

	category "routeProtocol"

	desc """获取路由器的ID"""

	rest {
		request {
			url "GET /v1/routerArea/{vRouterUuid}/routerid"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVRouterRouterIdMsg.class

			desc """"""

			params {

				column {
					name "vRouterUuid"
					enclosedIn ""
					desc "路由器的UUID，唯一标识"
					location "url"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.4"
				}
			}
		}

		response {
			clz APIGetVRouterRouterIdReply.class
		}
	}
}