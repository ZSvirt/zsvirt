package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIQueryLoadBalancerListenerReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询负载均衡监听器(QueryLoadBalancerListener)"

	category "负载均衡"

	desc """查询负载均衡监听器"""

	rest {
		request {
			url "GET /v1/load-balancers/listeners"
			url "GET /v1/load-balancers/listeners/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLoadBalancerListenerMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLoadBalancerListenerReply.class
		}
	}
}