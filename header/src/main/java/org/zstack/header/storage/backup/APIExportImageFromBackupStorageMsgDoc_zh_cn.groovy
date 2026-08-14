package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIExportImageFromBackupStorageEvent

doc {
	title "从镜像服务器导出镜像(ExportImageFromBackupStorage)"

	category "storage.backup"

	desc """从镜像服务器中导出镜像"""

	rest {
		request {
			url "PUT /v1/backup-storage/{backupStorageUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExportImageFromBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "backupStorageUuid"
					enclosedIn "exportImageFromBackupStorage"
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 1.7"
				}
				column {
					name "imageUuid"
					enclosedIn "exportImageFromBackupStorage"
					desc "镜像UUID"
					location "body"
					type "String"
					optional false
					since "zsv 1.7"
				}
				column {
					name "exportFormat"
					enclosedIn "exportImageFromBackupStorage"
					desc "导出镜像的保存格式"
					location "body"
					type "String"
					optional true
					since "zsv 3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 1.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 1.7"
				}
			}
		}

		response {
			clz APIExportImageFromBackupStorageEvent.class
		}
	}
}