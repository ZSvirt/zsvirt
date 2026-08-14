package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APIQueryImageStoreBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询镜像仓库服务器(QueryImageStoreBackupStorage)"

	category "storage.backup.imagestore"

	desc """查询镜像仓库服务器"""

	rest {
		request {
			url "GET /v1/backup-storage/image-store"
			url "GET /v1/backup-storage/image-store/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryImageStoreBackupStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryImageStoreBackupStorageReply.class
		}
	}
}