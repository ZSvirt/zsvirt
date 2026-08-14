package org.zstack.header.storage.database.backup

import org.zstack.header.storage.database.backup.APIGetDatabaseBackupFromImageStoreReply

doc {
	title "GetDatabaseBackupFromImageStore"

	category "backup.database"

	desc """获取备份服务器上的数据库备份信息"""

	rest {
		request {
			url "GET /v1/database-backups/image-store"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetDatabaseBackupFromImageStoreMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn ""
					desc "镜像服务器URL"
					location "query"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "registryPort"
					enclosedIn ""
					desc "镜像仓库访问端口"
					location "query"
					type "int"
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
			clz APIGetDatabaseBackupFromImageStoreReply.class
		}
	}
}