package org.zstack.storage.backup.sftp

import org.zstack.storage.backup.sftp.APIQuerySftpBackupStorageReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询Sftp镜像服务器(QuerySftpBackupStorage)"

	category "storage.backup.sftp"

	desc """查询所有的Sftp镜像服务器"""

	rest {
		request {
			url "GET /v1/backup-storage/sftp"
			url "GET /v1/backup-storage/sftp/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySftpBackupStorageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySftpBackupStorageReply.class
		}
	}
}