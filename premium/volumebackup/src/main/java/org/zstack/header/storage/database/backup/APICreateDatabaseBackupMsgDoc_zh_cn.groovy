package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APICreateDatabaseBackupEvent

doc {
	title "CreateDatabaseBackup"

	category "backup.database"

	desc """创建数据库备份"""

	rest {
		request {
			url "POST /v1/database-backups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateDatabaseBackupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateDatabaseBackupEvent.class
		}
	}
}