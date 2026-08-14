package org.zstack.network.hostNetworkInterface.lldp.api

import org.zstack.network.hostNetworkInterface.lldp.api.APIQueryHostNetworkInterfaceLldpReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryHostNetworkInterfaceLldp"

	category "hostNetwork.lldp"

	desc """查询物理网口lldp配置"""

	rest {
		request {
			url "GET /v1/hostNetworkInterface/lldp/all"
			url "GET /v1/hostNetworkInterface/lldp/{interfaceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryHostNetworkInterfaceLldpMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryHostNetworkInterfaceLldpReply.class
		}
	}
}