package org.zstack.network.service.eip

import org.zstack.network.service.eip.APIQueryEipReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询弹性IP(QueryEip)"

	category "弹性IP"

	desc """查询弹性IP"""

	rest {
		request {
			url "GET /v1/eips"
			url "GET /v1/eips/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEipMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryEipReply.class
		}
	}
}