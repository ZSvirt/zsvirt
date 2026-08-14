package org.zstack.header.protocol

import org.zstack.header.protocol.APIAddVRouterNetworksToOspfAreaEvent

doc {
	title "AddVRouterNetworksToOspfArea"

	category "routeProtocol"

	desc """添加网络到OSPF的区域"""

	rest {
		request {
			url "POST /v1/routerArea/{routerAreaUuid}/router/{vRouterUuid}/addnetworks"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddVRouterNetworksToOspfAreaMsg.class

			desc """"""

			params {

				column {
					name "routerAreaUuid"
					enclosedIn "params"
					desc "路由区域的id，唯一标识"
					location "url"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "vRouterUuid"
					enclosedIn "params"
					desc "vpc路由器的唯一标识id"
					location "url"
					type "String"
					optional false
					since "3.4"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "3层网络的唯一标识id"
					location "body"
					type "List"
					optional false
					since "3.4"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源的标识id"
					location "body"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "路由器系统标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "路由器用户标签"
					location "body"
					type "List"
					optional true
					since "3.4"
				}
			}
		}

		response {
			clz APIAddVRouterNetworksToOspfAreaEvent.class
		}
	}
}