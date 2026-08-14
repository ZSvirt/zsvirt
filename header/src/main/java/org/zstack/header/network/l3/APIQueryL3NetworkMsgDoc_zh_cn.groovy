package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIQueryL3NetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询三层网络(QueryL3Network)"

	category "三层网络"

	desc """查询三层网络"""

	rest {
		request {
			url "GET /v1/l3-networks"
			url "GET /v1/l3-networks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryL3NetworkMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryL3NetworkReply.class
		}
	}
}