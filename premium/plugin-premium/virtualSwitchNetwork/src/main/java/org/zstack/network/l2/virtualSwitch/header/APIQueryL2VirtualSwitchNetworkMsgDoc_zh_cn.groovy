package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIQueryL2VirtualSwitchNetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询虚拟交换机(QueryL2VirtualSwitchNetwork)"

	category "network.l2"

	desc """查询虚拟交换机"""

	rest {
		request {
			url "GET /v1/l2-networks/virtual-switch"
			url "GET /v1/l2-networks/virtual-switch/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryL2VirtualSwitchNetworkMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryL2VirtualSwitchNetworkReply.class
		}
	}
}