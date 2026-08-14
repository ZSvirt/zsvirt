package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APIRevertVmFromVmBackupEvent

doc {
	title "RevertVmFromVmBackup"

	category "backup.volume"

	desc """从虚拟机备份恢复虚拟机"""

	rest {
		request {
			url "PUT /v1/vm-backups/{groupUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRevertVmFromVmBackupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn "revertVmFromVmBackup"
					desc "备份组uuid"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "revertVmFromVmBackup"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "strategy"
					enclosedIn "revertVmFromVmBackup"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 4.3.0"
					values ("InstantStart","JustCreate","CreateStopped")
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
			clz APIRevertVmFromVmBackupEvent.class
		}
	}
}