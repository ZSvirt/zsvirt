package org.zstack.network.securitygroup

import org.zstack.network.securitygroup.APIQueryVmNicInSecurityGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmNicInSecurityGroup"

	category "securityGroup"

	desc """查询应用了安全组的网卡列表"""

	rest {
		request {
			url "GET /v1/security-groups/vm-instances/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmNicInSecurityGroupMsg.class

			desc """查询应用了安全组的网卡列表"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmNicInSecurityGroupReply.class
		}
	}
}