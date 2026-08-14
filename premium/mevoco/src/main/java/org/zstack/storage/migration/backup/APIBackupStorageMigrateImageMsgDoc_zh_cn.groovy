package org.zstack.storage.migration.backup

import org.zstack.storage.migration.backup.APIBackupStorageMigrateImageEvent

doc {
	title "BackupStorageMigrateImage"

	category "mevoco"

	desc """跨存储迁移镜像"""

	rest {
		request {
			url "PUT /v1/backup-storage/images/{imageUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBackupStorageMigrateImageMsg.class

			desc """跨存储迁移镜像"""

			params {

				column {
					name "imageUuid"
					enclosedIn "backupStorageMigrateImage"
					desc "镜像UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "backupStorageMigrateImage"
					desc "源备份存储UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "backupStorageMigrateImage"
					desc "目标备份存储UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIBackupStorageMigrateImageEvent.class
		}
	}
}