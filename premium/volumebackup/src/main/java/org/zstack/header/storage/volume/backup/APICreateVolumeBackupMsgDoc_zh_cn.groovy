package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APICreateVolumeBackupEvent

doc {
	title "CreateVolumeBackup"

	category "backup.volume"

	desc """创建卷备份"""

	rest {
		request {
			url "POST /v1/volumes/{volumeUuid}/volume-backups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVolumeBackupMsg.class

			desc """"""

			params {

				column {
					name "volumeUuid"
					enclosedIn "params"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "镜像存储UUID"
					location "body"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "指定资源uuid"
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
			clz APICreateVolumeBackupEvent.class
		}
	}
}