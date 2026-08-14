package org.zstack.header.protocol

import org.zstack.header.protocol.APISetVRouterRouterIdEvent

doc {
	title "SetVRouterRouterId"

	category "routeProtocol"

	desc """设置路由器的ID"""

	rest {
		request {
			url "POST /v1/routerArea/{vRouterUuid}/routerid"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVRouterRouterIdMsg.class

			desc """"""

			params {

				column {
					name "vRouterUuid"
					enclosedIn "params"
					desc "路由器的UUID，唯一标识"
					location "url"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "routerId"
					enclosedIn "params"
					desc "IP地址形式的ID"
					location "body"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
			}
		}

		response {
			clz APISetVRouterRouterIdEvent.class
		}
	}
}