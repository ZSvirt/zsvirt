package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIQueryVRouterRouteTableReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询云路由路由表"

	category "vrouterRoute"

	desc """查询全部云路由路由表"""

	rest {
		request {
			url "GET /v1/vrouter-route-tables"
			url "GET /v1/vrouter-route-tables/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVRouterRouteTableMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVRouterRouteTableReply.class
		}
	}
}