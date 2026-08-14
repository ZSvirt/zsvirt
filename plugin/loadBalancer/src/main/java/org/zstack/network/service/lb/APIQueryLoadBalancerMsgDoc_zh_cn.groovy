package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIQueryLoadBalancerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询负载均衡器(QueryLoadBalancer)"

	category "负载均衡"

	desc """查询负载均衡器"""

	rest {
		request {
			url "GET /v1/load-balancers"
			url "GET /v1/load-balancers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLoadBalancerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLoadBalancerReply.class
		}
	}
}