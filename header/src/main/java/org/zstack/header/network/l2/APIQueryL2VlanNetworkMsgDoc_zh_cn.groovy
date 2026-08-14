package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIQueryL2VlanNetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询二层Vlan网络(QueryL2VlanNetwork)"

	category "network.l2.vlan"

	desc """查询二层Vlan网络"""

	rest {
		request {
			url "GET /v1/l2-networks/vlan"
			url "GET /v1/l2-networks/vlan/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryL2VlanNetworkMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryL2VlanNetworkReply.class
		}
	}
}