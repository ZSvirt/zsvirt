package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIQueryIpAddressReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryIpAddress"

	category "network.l3"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/l3-networks/ip-address"
			url "GET /v1/l3-networks/ip-address/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryIpAddressMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryIpAddressReply.class
		}
	}
}