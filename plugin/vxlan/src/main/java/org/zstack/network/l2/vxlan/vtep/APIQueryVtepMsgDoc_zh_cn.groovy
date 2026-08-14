package org.zstack.network.l2.vxlan.vtep

import org.zstack.network.l2.vxlan.vtep.APIQueryVtepReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询VXLAN隧道端点(QueryVtep)"

	category "network.l2"

	desc """查询隧道端点"""

	rest {
		request {
			url "GET /v1/l2-networks/vteps"
			url "GET /v1/l2-networks/vteps/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVtepMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVtepReply.class
		}
	}
}