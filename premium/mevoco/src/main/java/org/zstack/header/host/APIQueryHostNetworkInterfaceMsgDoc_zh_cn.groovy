package org.zstack.header.host

import org.zstack.header.host.APIQueryHostNetworkInterfaceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostNetworkInterface"

	category "host"

	desc """查询物理机网卡信息"""

	rest {
		request {
			url "GET /v1/hosts/nics"
			url "GET /v1/hosts/nics/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostNetworkInterfaceMsg.class

			desc """查询物理机网卡信息"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostNetworkInterfaceReply.class
		}
	}
}