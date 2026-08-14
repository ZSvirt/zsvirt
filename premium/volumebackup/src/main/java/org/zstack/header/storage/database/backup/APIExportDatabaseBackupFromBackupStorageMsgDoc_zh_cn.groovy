package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APIExportDatabaseBackupFromBackupStorageEvent

doc {
	title "ExportDatabaseBackupFromBackupStorage"

	category "backup.database"

	desc """从备份服务器导出数据库备份"""

	rest {
		request {
			url "PUT /v1/database-backups/{databaseBackupUuid}/backup-storage/{backupStorageUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExportDatabaseBackupFromBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "backupStorageUuid"
					enclosedIn "exportDatabaseBackupFromBackupStorage"
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "databaseBackupUuid"
					enclosedIn "exportDatabaseBackupFromBackupStorage"
					desc "数据库备份UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
			}
		}

		response {
			clz APIExportDatabaseBackupFromBackupStorageEvent.class
		}
	}
}