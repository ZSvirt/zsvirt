package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIQueryUplinkGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询上行链路组(QueryUplinkGroup)"

	category "network.l2"

	desc """查询上行链路组"""

	rest {
		request {
			url "GET /v1/l2-networks/virtual-switch/uplink-group"
			url "GET /v1/l2-networks/virtual-switch/uplink-group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryUplinkGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryUplinkGroupReply.class
		}
	}
}