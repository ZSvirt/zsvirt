package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APICreateVmBackupEvent

doc {
	title "CreateVmBackup"

	category "backup.volume"

	desc """创建虚拟机备份"""

	rest {
		request {
			url "POST /v1/volumes/{rootVolumeUuid}/vm-backups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmBackupMsg.class

			desc """"""

			params {

				column {
					name "rootVolumeUuid"
					enclosedIn "params"
					desc "根云盘UUID"
					location "url"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.0.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
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
				column {
					name "volumeReadBandwidth"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "volumeWriteBandwidth"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "networkReadBandwidth"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "networkWriteBandwidth"
					enclosedIn "params"
					desc ""
					location "body"
					type "Long"
					optional true
					since "0.6"
				}
				column {
					name "mode"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("full")
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateVmBackupEvent.class
		}
	}
}