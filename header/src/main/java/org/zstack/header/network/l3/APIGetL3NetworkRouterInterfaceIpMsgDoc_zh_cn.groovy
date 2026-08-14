package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIGetL3NetworkRouterInterfaceIpReply

doc {
	title "GetL3NetworkRouterInterfaceIp"

	category "network.l3"

	desc """获取三层网络上路由器的接口地址"""

	rest {
		request {
			url "GET /v1/l3-networks/{l3NetworkUuid}/router-interface-ip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetL3NetworkRouterInterfaceIpMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 2.2"
				}
			}
		}

		response {
			clz APIGetL3NetworkRouterInterfaceIpReply.class
		}
	}
}