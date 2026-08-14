package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APISyncVmBackupFromImageStoreBackupStorageEvent

doc {
	title "SyncVmBackupFromImageStoreBackupStorage"

	category "backup.volume"

	desc """同步虚拟机备份至备份镜像服务器"""

	rest {
		request {
			url "PUT /v1/vm-backups/{groupUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncVmBackupFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn "syncVmBackupFromImageStoreBackupStorage"
					desc "备份组uuid"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "syncVmBackupFromImageStoreBackupStorage"
					desc "本地镜像服务器"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "syncVmBackupFromImageStoreBackupStorage"
					desc "备份镜像服务器"
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
			clz APISyncVmBackupFromImageStoreBackupStorageEvent.class
		}
	}
}