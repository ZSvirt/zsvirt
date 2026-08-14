package org.zstack.header.protocol

import org.zstack.header.protocol.APIRemoveVRouterNetworksFromOspfAreaEvent

doc {
	title "RemoveVRouterNetworksFromOspfArea"

	category "routeProtocol"

	desc """从路由区域中移除网络"""

	rest {
		request {
			url "DELETE /v1/routerArea/networks"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveVRouterNetworksFromOspfAreaMsg.class

			desc """"""

			params {

				column {
					name "uuids"
					enclosedIn ""
					desc "网络区域表中的uuid，全局唯一标识"
					location "query"
					type "List"
					optional false
					since "3.4"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.4"
				}
			}
		}

		response {
			clz APIRemoveVRouterNetworksFromOspfAreaEvent.class
		}
	}
}