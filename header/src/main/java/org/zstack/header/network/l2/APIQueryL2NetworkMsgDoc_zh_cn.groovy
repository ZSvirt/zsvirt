package org.zstack.header.network.l2

import org.zstack.header.network.l2.APIQueryL2NetworkReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询二层网络(QueryL2Network)"

	category "二层网络"

	desc """查询二层网络"""

	rest {
		request {
			url "GET /v1/l2-networks"
			url "GET /v1/l2-networks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryL2NetworkMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryL2NetworkReply.class
		}
	}
}