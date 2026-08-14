package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.crypto.keyprovider.nkp.api.APIQueryNkpReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryNkp"

	category "keyProvider"

	desc """查询原生密钥提供程序"""

	rest {
		request {
			url "GET /v1/key-providers/nkp"
			url "GET /v1/key-providers/nkp/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryNkpMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryNkpReply.class
		}
	}
}