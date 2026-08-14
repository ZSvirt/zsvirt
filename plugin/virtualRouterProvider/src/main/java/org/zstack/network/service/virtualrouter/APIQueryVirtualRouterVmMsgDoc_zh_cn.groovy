package org.zstack.network.service.virtualrouter

import org.zstack.appliancevm.APIQueryApplianceVmReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询虚拟路由器云主机(QueryVirtualRouterVm)"

	category "虚拟路由器"

	desc """查询虚拟路由器云主机"""

	rest {
		request {
			url "GET /v1/vm-instances/appliances/virtual-routers"
			url "GET /v1/vm-instances/appliances/virtual-routers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVirtualRouterVmMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryApplianceVmReply.class
		}
	}
}