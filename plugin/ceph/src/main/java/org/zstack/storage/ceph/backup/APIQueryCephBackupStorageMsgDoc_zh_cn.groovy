package org.zstack.storage.ceph.backup

import org.zstack.header.storage.backup.APIQueryBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询 Ceph 镜像服务器(QueryCephBackupStorage)"

	category "storage.ceph.backup"

	desc """查询 Ceph 镜像服务器"""

	rest {
		request {
			url "GET /v1/backup-storage/ceph"
			url "GET /v1/backup-storage/ceph/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryCephBackupStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBackupStorageReply.class
		}
	}
}