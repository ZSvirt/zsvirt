package org.zstack.storage.primary.block.message

import org.zstack.storage.primary.block.message.APIQueryBlockPrimaryStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryBlockPrimaryStorage"

	category "storage.primary"

	desc """查询Block主存储"""

	rest {
		request {
			url "GET /v1/primary-storage/block"
			url "GET /v1/primary-storage/block/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryBlockPrimaryStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBlockPrimaryStorageReply.class
		}
	}
}