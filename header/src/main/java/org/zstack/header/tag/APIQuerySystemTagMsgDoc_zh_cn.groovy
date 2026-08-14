package org.zstack.header.tag

import org.zstack.header.tag.APIQuerySystemTagReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySystemTag"

	category "tag"

	desc """查询系统标签"""

	rest {
		request {
			url "GET /v1/system-tags"
			url "GET /v1/system-tags/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySystemTagMsg.class

			desc """查询系统标签"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySystemTagReply.class
		}
	}
}