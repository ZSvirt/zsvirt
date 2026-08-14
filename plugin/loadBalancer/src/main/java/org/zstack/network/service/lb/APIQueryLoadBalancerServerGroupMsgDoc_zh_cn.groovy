package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIQueryLoadBalancerServerGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryLoadBalancerServerGroup"

	category "loadBalancer"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/load-balancers/servergroups"
			url "GET /v1/load-balancers/servergroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLoadBalancerServerGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLoadBalancerServerGroupReply.class
		}
	}
}