package org.zstack.header.identity

import org.zstack.header.identity.APIQueryAccountReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccount"

	category "identity"

	desc """查询账户"""

	rest {
		request {
			url "GET /v1/accounts"
			url "GET /v1/accounts/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccountMsg.class

			desc """查询账户"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccountReply.class
		}
	}
}