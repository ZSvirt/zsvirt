package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APICreateDataVolumeFromVolumeBackupEvent

doc {
	title "CreateDataVolumeFromVolumeBackup"

	category "backup.volume"

	desc """从备份创建数据云盘"""

	rest {
		request {
			url "POST /v1/volumes/data-volume/from/volume-template/{backupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateDataVolumeFromVolumeBackupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "数据云盘名称"
					location "body"
					type "String"
					optional false
					since "3.18.0"
				}
				column {
					name "vmUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
					type "String"
					optional false
					since "3.18.0"
				}
				column {
					name "backupUuid"
					enclosedIn "params"
					desc "备份UUID"
					location "url"
					type "String"
					optional false
					since "3.18.0"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "备份服务器UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
			}
		}

		response {
			clz APICreateDataVolumeFromVolumeBackupEvent.class
		}
	}
}