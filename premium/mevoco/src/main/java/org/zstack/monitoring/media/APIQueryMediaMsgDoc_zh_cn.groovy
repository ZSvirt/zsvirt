package org.zstack.monitoring.media

import org.zstack.monitoring.media.APIQueryMediaReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMedia"

	category "monitoring"

	desc """查询媒体"""

	rest {
		request {
			url "GET /v1/media"
			url "GET /v1/media/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMediaMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMediaReply.class
		}
	}
}