package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIQueryCephPrimaryStoragePoolReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryCephPrimaryStoragePool"

	category "storage.ceph.primary"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/primary-storage/ceph/pools"
			url "GET /v1/primary-storage/ceph/pools/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryCephPrimaryStoragePoolMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryCephPrimaryStoragePoolReply.class
		}
	}
}