package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.APIQuerySshKeyPairReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySshKeyPair"

	category "sshKeyPair"

	desc """查询密钥对"""

	rest {
		request {
			url "GET /v1/ssh-key-pair"
			url "GET /v1/ssh-key-pair/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySshKeyPairMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySshKeyPairReply.class
		}
	}
}