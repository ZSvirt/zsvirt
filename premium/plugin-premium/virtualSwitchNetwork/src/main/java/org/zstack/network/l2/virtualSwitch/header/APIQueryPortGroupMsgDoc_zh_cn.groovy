package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIQueryPortGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询端口组(QueryPortGroup)"

	category "network.l2"

	desc """查询端口组"""

	rest {
		request {
			url "GET /v1/l3-networks/port-group"
			url "GET /v1/l3-networks/port-group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPortGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPortGroupReply.class
		}
	}
}