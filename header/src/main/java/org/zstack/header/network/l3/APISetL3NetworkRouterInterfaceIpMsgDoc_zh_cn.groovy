package org.zstack.header.network.l3

import org.zstack.header.network.l3.APISetL3NetworkRouterInterfaceIpEvent

doc {
	title "SetL3NetworkRouterInterfaceIp"

	category "network.l3"

	desc """设置三层网络上路由器接口地址，仅当会在普通三层网络上创建云路由器或在VPC网络上加载VPC路由器时有效"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/router-interface-ip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetL3NetworkRouterInterfaceIpMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 2.2"
				}
				column {
					name "routerInterfaceIp"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "zsv 2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.2"
				}
			}
		}

		response {
			clz APISetL3NetworkRouterInterfaceIpEvent.class
		}
	}
}