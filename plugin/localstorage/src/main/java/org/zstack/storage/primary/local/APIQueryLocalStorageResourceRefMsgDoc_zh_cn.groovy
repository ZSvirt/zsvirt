package org.zstack.storage.primary.local

import org.zstack.storage.primary.local.APIQueryLocalStorageResourceRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询本地存储资源引用(QueryLocalStorageResourceRef)"

	category "storage.primary"

	desc """查询本地存储资源引用"""

	rest {
		request {
			url "GET /v1/primary-storage/local-storage/resource-refs"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryLocalStorageResourceRefMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryLocalStorageResourceRefReply.class
		}
	}
}