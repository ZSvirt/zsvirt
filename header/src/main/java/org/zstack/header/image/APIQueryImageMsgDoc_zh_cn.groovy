package org.zstack.header.image

import org.zstack.header.image.APIQueryImageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryImage"

	category "image"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/images"
			url "GET /v1/images/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryImageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryImageReply.class
		}
	}
}