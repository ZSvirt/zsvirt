package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIQueryL2PortGroupNetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询端口组L2(QueryL2PortGroupNetwork)"

	category "network.l2"

	desc """查询端口组L2"""

	rest {
		request {
			url "GET /v1/l2-networks/port-group"
			url "GET /v1/l2-networks/port-group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryL2PortGroupNetworkMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryL2PortGroupNetworkReply.class
		}
	}
}