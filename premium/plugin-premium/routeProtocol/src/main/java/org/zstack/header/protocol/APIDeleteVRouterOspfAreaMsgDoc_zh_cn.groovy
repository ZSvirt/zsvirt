package org.zstack.header.protocol

import org.zstack.header.protocol.APIDeleteVRouterOspfAreaEvent

doc {
	title "DeleteVRouterOspfArea"

	category "routeProtocol"

	desc """删除OSPF路由区域"""

	rest {
		request {
			url "DELETE /v1/routerArea/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVRouterOspfAreaMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
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
			clz APIDeleteVRouterOspfAreaEvent.class
		}
	}
}