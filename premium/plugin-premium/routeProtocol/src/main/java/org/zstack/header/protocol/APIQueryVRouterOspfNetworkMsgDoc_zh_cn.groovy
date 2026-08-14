package org.zstack.header.protocol

import org.zstack.header.protocol.APIQueryVRouterOspfNetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVRouterOspfNetwork"

	category "routeProtocol"

	desc """查询路由加入到区域的网络信息"""

	rest {
		request {
			url "GET /v1/routerArea/network"
			url "GET /v1/routerArea/networkR/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVRouterOspfNetworkMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVRouterOspfNetworkReply.class
		}
	}
}