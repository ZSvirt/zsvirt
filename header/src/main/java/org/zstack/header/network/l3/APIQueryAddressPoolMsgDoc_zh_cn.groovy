package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIQueryAddressPoolReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询IP地址池(QueryAddressPool)"

	category "三层网络"

	desc """查询IP地址池"""

	rest {
		request {
			url "GET /v1/l3-networks/address-pools"
			url "GET /v1/l3-networks/address-pools/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAddressPoolMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAddressPoolReply.class
		}
	}
}