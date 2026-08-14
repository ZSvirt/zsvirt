package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APIRecoverDatabaseFromBackupEvent

doc {
	title "RecoverDatabaseFromBackup"

	category "backup.database"

	desc """从数据库备份恢复数据库"""

	rest {
		request {
			url "PUT /v1/database-backups/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRecoverDatabaseFromBackupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "recoverDatabaseFromBackup"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "backupStorageUrl"
					enclosedIn "recoverDatabaseFromBackup"
					desc "镜像服务器URL"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "backupInstallPath"
					enclosedIn "recoverDatabaseFromBackup"
					desc "数据库备份存储路径"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "mysqlRootPassword"
					enclosedIn "recoverDatabaseFromBackup"
					desc "MYSQL数据库ROOT密码"
					location "body"
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
			clz APIRecoverDatabaseFromBackupEvent.class
		}
	}
}