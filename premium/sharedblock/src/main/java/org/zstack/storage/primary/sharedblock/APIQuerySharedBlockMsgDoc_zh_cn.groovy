package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIQuerySharedBlockReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySharedBlock"

	category "storage.primary"

	desc """查询共享块设备"""

	rest {
		request {
			url "GET /v1/sharedblock-group/sharedblocks"
			url "GET /v1/sharedblock-group"
			url "GET /v1/sharedblock-group/sharedblock/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySharedBlockMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySharedBlockReply.class
		}
	}
}