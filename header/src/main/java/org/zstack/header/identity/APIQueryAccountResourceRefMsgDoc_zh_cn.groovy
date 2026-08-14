package org.zstack.header.identity

import org.zstack.header.identity.APIQueryAccountResourceRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccountResourceRef"

	category "identity"

	desc """查询账户资源引用"""

	rest {
		request {
			url "GET /v1/accounts/resources/refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccountResourceRefMsg.class

			desc """查询账户资源引用"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccountResourceRefReply.class
		}
	}
}