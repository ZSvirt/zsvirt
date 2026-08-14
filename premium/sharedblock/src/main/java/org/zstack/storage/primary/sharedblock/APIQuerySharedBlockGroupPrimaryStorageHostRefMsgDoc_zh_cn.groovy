package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIQuerySharedBlockGroupPrimaryStorageHostRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySharedBlockGroupPrimaryStorageHostRef"

	category "storage.primary"

	desc """查询共享块设备主存储物理机连接状态"""

	rest {
		request {
			url "GET /v1/sharedblock-group/host-refs"
			url "GET /v1/sharedblock-group/{primaryStorageUuid}/host-refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySharedBlockGroupPrimaryStorageHostRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySharedBlockGroupPrimaryStorageHostRefReply.class
		}
	}
}