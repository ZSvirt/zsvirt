package org.zstack.header.host

import org.zstack.header.host.APIQueryHostNetworkBondingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostNetworkBonding"

	category "host"

	desc """查询物理机Bond信息"""

	rest {
		request {
			url "GET /v1/hosts/bondings"
			url "GET /v1/hosts/bondings/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostNetworkBondingMsg.class

			desc """查询物理机Bond信息"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostNetworkBondingReply.class
		}
	}
}