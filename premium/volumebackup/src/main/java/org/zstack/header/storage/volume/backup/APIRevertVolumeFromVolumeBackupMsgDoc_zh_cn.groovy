package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APIRevertVolumeFromVolumeBackupEvent

doc {
	title "RevertVolumeFromVolumeBackup"

	category "backup.volume"

	desc """从卷备份恢复卷"""

	rest {
		request {
			url "PUT /v1/volume-backups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRevertVolumeFromVolumeBackupMsg.class

			desc """"""

			params {

				column {
					name "卷备份uuid"
					enclosedIn "revertVolumeFromVolumeBackup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "revertVolumeFromVolumeBackup"
					desc "镜像服务器uuid"
					location "body"
					type "String"
					optional true
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
			clz APIRevertVolumeFromVolumeBackupEvent.class
		}
	}
}