package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APISyncVolumeBackupEvent

doc {
	title "SyncVolumeBackup"

	category "backup.volume"

	desc """扫描云盘备份"""

	rest {
		request {
			url "PUT /v1/volume-backups/imageStore/{imageStoreUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncVolumeBackupMsg.class

			desc """"""

			params {

				column {
					name "imageStoreUuid"
					enclosedIn "syncVolumeBackup"
					desc "备份服务器UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APISyncVolumeBackupEvent.class
		}
	}
}