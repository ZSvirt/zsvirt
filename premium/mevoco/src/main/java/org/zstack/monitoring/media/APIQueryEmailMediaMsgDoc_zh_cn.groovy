package org.zstack.monitoring.media

import org.zstack.monitoring.media.APIQueryMediaReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryEmailMedia"

	category "monitoring"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/media/emails"
			url "GET /v1/media/emails/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryEmailMediaMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMediaReply.class
		}
	}
}