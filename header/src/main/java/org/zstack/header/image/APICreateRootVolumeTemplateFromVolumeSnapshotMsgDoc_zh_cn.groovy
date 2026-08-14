package org.zstack.header.image

import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotEvent

doc {
	title "从云盘快照创建根云盘镜像(CreateRootVolumeTemplateFromVolumeSnapshot)"

	category "image"

	desc """从云盘快照创建根云盘镜像"""

	rest {
		request {
			url "POST /v1/images/root-volume-templates/from/volume-snapshots/{snapshotUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateRootVolumeTemplateFromVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "snapshotUuid"
					enclosedIn "params"
					desc "快照UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "根云盘名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "根云盘的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "guestOsType"
					enclosedIn "params"
					desc "根云盘客户机操作系统类型"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "backupStorageUuids"
					enclosedIn "params"
					desc "镜像服务器UUID列表"
					location "body"
					type "List"
					optional false
					since "zsv 0.6"
				}
				column {
					name "platform"
					enclosedIn "params"
					desc "根云盘系统平台"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
				}
				column {
					name "system"
					enclosedIn "params"
					desc "是否系统根云盘"
					location "body"
					type "boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "根云盘UUID。若指定，根云盘会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统镜像"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "architecture"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.10"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
				column {
					name "virtio"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "zsv 3.11.2"
				}
			}
		}

		response {
			clz APICreateRootVolumeTemplateFromVolumeSnapshotEvent.class
		}
	}
}