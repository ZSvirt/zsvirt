package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIQueryVmNicSecurityPolicyReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmNicSecurityPolicy"

	category "securityGroup"

	desc """查询网卡的安全策略"""

	rest {
		request {
			url "GET /v1/security-groups/nics/security-policy"
			url "GET /v1/security-groups/nics/{uuid}/security-policy"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmNicSecurityPolicyMsg.class

			desc """查询网卡的安全策略"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmNicSecurityPolicyReply.class
		}
	}
}