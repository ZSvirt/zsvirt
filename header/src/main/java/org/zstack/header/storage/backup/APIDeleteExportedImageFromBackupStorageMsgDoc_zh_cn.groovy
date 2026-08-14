package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APIDeleteExportedImageFromBackupStorageEvent

doc {
	title "从镜像服务器删除导出的镜像(DeleteExportedImageFromBackupStorage)"

	category "storage.backup"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/backup-storage/{backupStorageUuid}/exported-images/{imageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteExportedImageFromBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "backupStorageUuid"
					enclosedIn ""
					desc "镜像存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 1.7"
				}
				column {
					name "imageUuid"
					enclosedIn ""
					desc "镜像UUID"
					location "url"
					type "String"
					optional false
					since "zsv 1.7"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 1.7"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 1.7"
				}
			}
		}

		response {
			clz APIDeleteExportedImageFromBackupStorageEvent.class
		}
	}
}