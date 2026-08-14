package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIQueryBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询镜像服务器(QueryBackupStorage)"

	category "storage.backup"

	desc """查询镜像服务器"""

	rest {
		request {
			url "GET /v1/backup-storage"
			url "GET /v1/backup-storage/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryBackupStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBackupStorageReply.class
		}
	}
}