package org.zstack.vrouterRoute

import org.zstack.vrouterRoute.APIQueryVRouterRouteEntryReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询云路由路由条目"

	category "vrouterRoute"

	desc """查询全部云路由路由条目"""

	rest {
		request {
			url "GET /v1/vrouter-route-tables/route-entries"
			url "GET /v1/vrouter-route-tables/route-entries/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVRouterRouteEntryMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVRouterRouteEntryReply.class
		}
	}
}