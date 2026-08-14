package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APIRecoveryImageFromImageStoreBackupStorageEvent

doc {
	title "RecoveryImageFromImageStoreBackupStorage"

	category "storage.backup.imagestore"

	desc """从灾备服务器中恢复镜像到镜像服务器"""

	rest {
		request {
			url "PUT /v1/backup-storage/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRecoveryImageFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "recoveryImageFromImageStoreBackupStorage"
					desc "要恢复镜像的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "recoveryImageFromImageStoreBackupStorage"
					desc "灾备服务器UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "recoveryImageFromImageStoreBackupStorage"
					desc "目标镜像服务器UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "name"
					enclosedIn "recoveryImageFromImageStoreBackupStorage"
					desc "恢复后的镜像名称"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "description"
					enclosedIn "recoveryImageFromImageStoreBackupStorage"
					desc "恢复后的镜像描述"
					location "body"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "镜像标签"
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
			clz APIRecoveryImageFromImageStoreBackupStorageEvent.class
		}
	}
}