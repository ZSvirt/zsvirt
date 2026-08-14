package org.zstack.crypto.keyprovider.kms.api

import org.zstack.crypto.keyprovider.kms.api.APIQueryKmsReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryKms"

	category "keyProvider"

	desc """查询 KMS 密钥提供程序"""

	rest {
		request {
			url "GET /v1/key-providers/kms"
			url "GET /v1/key-providers/kms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryKmsMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryKmsReply.class
		}
	}
}