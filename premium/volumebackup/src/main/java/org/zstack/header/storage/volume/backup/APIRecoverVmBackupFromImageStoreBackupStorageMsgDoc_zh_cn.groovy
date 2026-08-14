package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APIRecoverVmBackupFromImageStoreBackupStorageEvent

doc {
	title "RecoverVmBackupFromImageStoreBackupStorage"

	category "backup.volume"

	desc """从备份镜像仓库恢复虚拟机备份"""

	rest {
		request {
			url "PUT /v1/vm-backups/{groupUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRecoverVmBackupFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn "recoverVmBackupFromImageStoreBackupStorage"
					desc "虚拟机备份组uuid"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "recoverVmBackupFromImageStoreBackupStorage"
					desc "本地镜像仓库"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "recoverVmBackupFromImageStoreBackupStorage"
					desc "备份镜像仓库"
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
			clz APIRecoverVmBackupFromImageStoreBackupStorageEvent.class
		}
	}
}