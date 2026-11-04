package org.zstack.accessKey

import org.zstack.accessKey.APIQueryAccessKeyReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAccessKey"

	category "accessKey"

	desc """查询AccessKey"""

	rest {
		request {
			url "GET /v1/accesskeys"
			url "GET /v1/accesskeys/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAccessKeyMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAccessKeyReply.class
		}
	}
}