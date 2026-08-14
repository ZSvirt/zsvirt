package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APIRecoverBackupFromImageStoreBackupStorageEvent

doc {
	title "RecoverBackupFromImageStoreBackupStorage"

	category "backup.volume"

	desc """从目标镜像服务器恢复卷备份"""

	rest {
		request {
			url "PUT /v1/volume-backups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRecoverBackupFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "recoverBackupFromImageStoreBackupStorage"
					desc "卷备份的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "recoverBackupFromImageStoreBackupStorage"
					desc "接收恢复卷的镜像服务器uuid"
					location "body"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "recoverBackupFromImageStoreBackupStorage"
					desc "恢复镜像服务器uuid"
					location "body"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.6"
				}
			}
		}

		response {
			clz APIRecoverBackupFromImageStoreBackupStorageEvent.class
		}
	}
}