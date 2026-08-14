package org.zstack.header.protocol

import org.zstack.header.protocol.APIQueryVRouterOspfAreaReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVRouterOspfArea"

	category "routeProtocol"

	desc """查询OSPF路由区域信息"""

	rest {
		request {
			url "GET /v1/routerArea"
			url "GET /v1/routerArea/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVRouterOspfAreaMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVRouterOspfAreaReply.class
		}
	}
}