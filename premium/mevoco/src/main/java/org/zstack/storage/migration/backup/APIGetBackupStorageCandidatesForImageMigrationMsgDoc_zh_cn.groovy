package org.zstack.storage.migration.backup

import org.zstack.storage.migration.backup.APIGetBackupStorageCandidatesForImageMigrationReply

doc {
	title "GetBackupStorageCandidatesForImageMigration"

	category "mevoco"

	desc """获取镜像迁移的可选备份存储列表"""

	rest {
		request {
			url "GET /v1/backup-storage/{srcBackupStorageUuid}/migration-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetBackupStorageCandidatesForImageMigrationMsg.class

			desc """获取镜像迁移的可选备份存储列表"""

			params {

				column {
					name "srcBackupStorageUuid"
					enclosedIn ""
					desc "源备份存储UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIGetBackupStorageCandidatesForImageMigrationReply.class
		}
	}
}