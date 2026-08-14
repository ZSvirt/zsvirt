package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APISyncDatabaseBackupFromImageStoreBackupStorageEvent

doc {
	title "SyncDatabaseBackupFromImageStoreBackupStorage"

	category "backup.database"

	desc """同步数据库备份至备份数据库"""

	rest {
		request {
			url "PUT /v1/database-backups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncDatabaseBackupFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "syncDatabaseBackupFromImageStoreBackupStorage"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "syncDatabaseBackupFromImageStoreBackupStorage"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "syncDatabaseBackupFromImageStoreBackupStorage"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISyncDatabaseBackupFromImageStoreBackupStorageEvent.class
		}
	}
}