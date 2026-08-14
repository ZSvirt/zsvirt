package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APISyncBackupFromImageStoreBackupStorageEvent

doc {
	title "SyncBackupFromImageStoreBackupStorage"

	category "backup.volume"

	desc """将卷备份同步至目标镜像服务器"""

	rest {
		request {
			url "PUT /v1/volume-backups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncBackupFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "syncBackupFromImageStoreBackupStorage"
					desc "卷备份的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "syncBackupFromImageStoreBackupStorage"
					desc "源镜像服务器"
					location "body"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "syncBackupFromImageStoreBackupStorage"
					desc "目标镜像服务器"
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
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.6"
				}
			}
		}

		response {
			clz APISyncBackupFromImageStoreBackupStorageEvent.class
		}
	}
}