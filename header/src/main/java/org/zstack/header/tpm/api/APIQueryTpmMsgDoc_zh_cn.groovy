package org.zstack.header.tpm.api

import org.zstack.header.tpm.api.APIQueryTpmReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryTpm"

	category "tpm"

	desc """查询 TPM"""

	rest {
		request {
			url "GET /v1/tpms"
			url "GET /v1/tpms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryTpmMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryTpmReply.class
		}
	}
}