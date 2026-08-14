package org.zstack.network.service.virtualrouter

import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询虚拟路由器规格(QueryVirtualRouterOffering)"

	category "虚拟路由器"

	desc """查询虚拟路由器规格"""

	rest {
		request {
			url "GET /v1/instance-offerings/virtual-routers"
			url "GET /v1/instance-offerings/virtual-routers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVirtualRouterOfferingMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVirtualRouterOfferingReply.class
		}
	}
}