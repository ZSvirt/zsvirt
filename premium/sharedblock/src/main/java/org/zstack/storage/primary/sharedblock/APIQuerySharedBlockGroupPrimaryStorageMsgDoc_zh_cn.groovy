package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIQuerySharedBlockGroupPrimaryStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySharedBlockGroupPrimaryStorage"

	category "storage.primary"

	desc """查询共享块设备主存储"""

	rest {
		request {
			url "GET /v1/primary-storage/sharedblockgroup"
			url "GET /v1/primary-storage/sharedblockgroup/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySharedBlockGroupPrimaryStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySharedBlockGroupPrimaryStorageReply.class
		}
	}
}