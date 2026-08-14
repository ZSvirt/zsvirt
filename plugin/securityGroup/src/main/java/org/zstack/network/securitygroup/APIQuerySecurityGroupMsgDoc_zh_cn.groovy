package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIQuerySecurityGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySecurityGroup"

	category "securityGroup"

	desc """查询安全组"""

	rest {
		request {
			url "GET /v1/security-groups"
			url "GET /v1/security-groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySecurityGroupMsg.class

			desc """查询安全组"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySecurityGroupReply.class
		}
	}
}