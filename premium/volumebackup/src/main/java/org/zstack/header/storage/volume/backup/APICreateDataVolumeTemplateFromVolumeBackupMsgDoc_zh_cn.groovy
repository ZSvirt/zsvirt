package org.zstack.header.storage.volume.backup

import org.zstack.header.storage.volume.backup.APICreateDataVolumeTemplateFromVolumeBackupEvent

doc {
	title "CreateDataVolumeTemplateFromVolumeBackup"

	category "backup.volume"

	desc """从卷备份创建数据盘镜像"""

	rest {
		request {
			url "POST /v1/images/data-volume-templates/from/volume-template/{backupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateDataVolumeTemplateFromVolumeBackupMsg.class

			desc """"""

			params {

				column {
					name "backupUuid"
					enclosedIn "params"
					desc "卷备份uuid"
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
					name "guestOsType"
					enclosedIn "params"
					desc "宿主机操作系统类型"
					location "body"
					type "String"
					optional true
					since "2.6"
				}
				column {
					name "platform"
					enclosedIn "params"
					desc "宿主机平台类型"
					location "body"
					type "String"
					optional true
					since "2.6"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
				}
				column {
					name "system"
					enclosedIn "params"
					desc "是否系统镜像"
					location "body"
					type "boolean"
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
					name "architecture"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.10"
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
				column {
					name "virtio"
					enclosedIn "params"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "3.11.2"
				}
			}
		}

		response {
			clz APICreateDataVolumeTemplateFromVolumeBackupEvent.class
		}
	}
}