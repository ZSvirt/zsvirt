package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APISyncVmBackupEvent

doc {
	title "SyncVmBackup"

	category "backup.volume"

	desc """扫描云主机备份"""

	rest {
		request {
			url "PUT /v1/vm-backups/imageStore/{imageStoreUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncVmBackupMsg.class

			desc """"""

			params {

				column {
					name "imageStoreUuid"
					enclosedIn "syncVmBackup"
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
			clz APISyncVmBackupEvent.class
		}
	}
}