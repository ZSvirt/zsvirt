package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIQueryVirtualRouterVRouterRouteTableRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询云路由设备与云路由路由表绑定关系"

	category "vrouterRoute"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vrouter-route-tables/virtual-router-refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVirtualRouterVRouterRouteTableRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVirtualRouterVRouterRouteTableRefReply.class
		}
	}
}