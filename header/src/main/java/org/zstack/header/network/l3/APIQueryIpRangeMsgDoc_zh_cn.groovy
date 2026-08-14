package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIQueryIpRangeReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询IP地址范围(QueryIpRange)"

	category "三层网络"

	desc """查询IP地址范围"""

	rest {
		request {
			url "GET /v1/l3-networks/ip-ranges"
			url "GET /v1/l3-networks/ip-ranges/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryIpRangeMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryIpRangeReply.class
		}
	}
}