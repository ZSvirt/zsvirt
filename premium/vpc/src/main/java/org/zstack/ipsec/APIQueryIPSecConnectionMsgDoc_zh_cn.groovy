package org.zstack.ipsec

import org.zstack.ipsec.APIQueryIPSecConnectionReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询IPSec连接(QueryIPSecConnection)"

	category "ipsec"

	desc """查询IPSec连接"""

	rest {
		request {
			url "GET /v1/ipsec"
			url "GET /v1/ipsec/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryIPSecConnectionMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryIPSecConnectionReply.class
		}
	}
}