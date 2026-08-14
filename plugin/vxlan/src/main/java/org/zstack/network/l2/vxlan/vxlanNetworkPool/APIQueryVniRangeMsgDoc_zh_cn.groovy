package org.zstack.network.l2.vxlan.vxlanNetworkPool

import org.zstack.network.l2.vxlan.vxlanNetworkPool.APIQueryVniRangeReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询VNI范围(QueryVniRange)"

	category "network.l2"

	desc """查询VNI范围"""

	rest {
		request {
			url "GET /v1/l2-networks/vxlan-pool/vni-range"
			url "GET /v1/l2-networks/vxlan-pool/vni-range/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVniRangeMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVniRangeReply.class
		}
	}
}