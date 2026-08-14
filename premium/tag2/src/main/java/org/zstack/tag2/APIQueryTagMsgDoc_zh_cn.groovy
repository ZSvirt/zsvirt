package org.zstack.tag2

import org.zstack.tag2.APIQueryTagReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryTag"

	category "tag2"

	desc """查询标签"""

	rest {
		request {
			url "GET /v1/tags"
			url "GET /v1/tags/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryTagMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryTagReply.class
		}
	}
}