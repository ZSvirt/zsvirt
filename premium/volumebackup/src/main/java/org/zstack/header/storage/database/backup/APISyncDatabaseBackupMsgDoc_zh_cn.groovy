package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APISyncDatabaseBackupEvent

doc {
	title "SyncDatabaseBackup"

	category "backup.database"

	desc """扫描数据库备份"""

	rest {
		request {
			url "PUT /v1/database-backups/imageStore/{imageStoreUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncDatabaseBackupMsg.class

			desc """"""

			params {

				column {
					name "imageStoreUuid"
					enclosedIn "syncDatabaseBackup"
					desc "备份服务器UUID"
					location "url"
					type "String"
					optional false
					since "3.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.2.0"
				}
			}
		}

		response {
			clz APISyncDatabaseBackupEvent.class
		}
	}
}