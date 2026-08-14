package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryPrimaryStorage"

	category "storage.primary"

	desc """查询主存储"""

	rest {
		request {
			url "GET /v1/primary-storage"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPrimaryStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPrimaryStorageReply.class
		}
	}
}