package org.zstack.storage.ceph.primary

import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询 Ceph 主存储(QueryCephPrimaryStorage)"

	category "storage.ceph.primary"

	desc """查询 Ceph 主存储"""

	rest {
		request {
			url "GET /v1/primary-storage/ceph"
			url "GET /v1/primary-storage/ceph/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryCephPrimaryStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPrimaryStorageReply.class
		}
	}
}