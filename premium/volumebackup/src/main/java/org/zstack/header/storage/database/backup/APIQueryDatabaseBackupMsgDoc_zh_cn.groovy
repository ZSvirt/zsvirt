package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APIQueryDatabaseBackupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryDatabaseBackup"

	category "backup.database"

	desc """查询数据库备份"""

	rest {
		request {
			url "GET /v1/database-backups"
			url "GET /v1/database-backups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryDatabaseBackupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryDatabaseBackupReply.class
		}
	}
}