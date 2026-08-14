package org.zstack.network.service.portforwarding

import org.zstack.network.service.portforwarding.APIQueryPortForwardingRuleReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryPortForwardingRule"

	category "portForwarding"

	desc """用户可以使用QueryPortForwardingRule来查询规则"""

	rest {
		request {
			url "GET /v1/port-forwarding"
			url "GET /v1/port-forwarding/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPortForwardingRuleMsg.class

			desc """用户可以使用QueryPortForwardingRule来查询规则"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPortForwardingRuleReply.class
		}
	}
}