package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APIDeleteExportedDatabaseBackupFromBackupStorageEvent

doc {
	title "DeleteExportedDatabaseBackupFromBackupStorage"

	category "backup.database"

	desc """从备份服务器删除导出的数据库备份"""

	rest {
		request {
			url "DELETE /v1/exported-database-backup/{databaseBackupUuid}/backup-storage/{backupStorageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteExportedDatabaseBackupFromBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "backupStorageUuid"
					enclosedIn ""
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "databaseBackupUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.0.0"
				}
			}
		}

		response {
			clz APIDeleteExportedDatabaseBackupFromBackupStorageEvent.class
		}
	}
}