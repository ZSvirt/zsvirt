package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIQuerySecurityGroupRuleReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySecurityGroupRule"

	category "securityGroup"

	desc """查询安全组规则"""

	rest {
		request {
			url "GET /v1/security-groups/rules"
			url "GET /v1/security-groups/rules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySecurityGroupRuleMsg.class

			desc """查询安全组规则"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySecurityGroupRuleReply.class
		}
	}
}