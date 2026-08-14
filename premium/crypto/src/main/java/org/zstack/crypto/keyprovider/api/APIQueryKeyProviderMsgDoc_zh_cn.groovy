package org.zstack.crypto.keyprovider.api

import org.zstack.crypto.keyprovider.api.APIQueryKeyProviderReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryKeyProvider"

	category "keyProvider"

	desc """查询密钥提供程序"""

	rest {
		request {
			url "GET /v1/key-providers"
			url "GET /v1/key-providers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryKeyProviderMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryKeyProviderReply.class
		}
	}
}