package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APIDeleteDatabaseBackupEvent

doc {
	title "DeleteDatabaseBackup"

	category "backup.database"

	desc """删除数据库备份"""

	rest {
		request {
			url "DELETE /v1/database-backups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteDatabaseBackupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "backupStorageUuids"
					enclosedIn ""
					desc "镜像服务器UUID列表"
					location "query"
					type "List"
					optional true
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
			clz APIDeleteDatabaseBackupEvent.class
		}
	}
}