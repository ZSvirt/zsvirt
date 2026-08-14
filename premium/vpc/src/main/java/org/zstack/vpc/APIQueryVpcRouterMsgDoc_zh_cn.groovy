package org.zstack.vpc

import org.zstack.vpc.APIQueryVpcRouterReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVpcRouter"

	category "vpc"

	desc """查询VPC云路由"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers"
			url "GET /v1/vpc/virtual-routers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVpcRouterMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVpcRouterReply.class
		}
	}
}