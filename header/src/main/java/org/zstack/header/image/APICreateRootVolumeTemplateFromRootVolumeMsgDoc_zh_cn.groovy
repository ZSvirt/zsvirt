package org.zstack.header.image

import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeEvent

doc {
	title "从根云盘创建根云盘镜像(CreateRootVolumeTemplateFromRootVolume)"

	category "image"

	desc """从根云盘创建根云盘镜像"""

	rest {
		request {
			url "POST /v1/images/root-volume-templates/from/volumes/{rootVolumeUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateRootVolumeTemplateFromRootVolumeMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "根云盘镜像名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "根云盘镜像的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "guestOsType"
					enclosedIn "params"
					desc "根云盘镜像对应客户机操作系统类型"
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
					optional true
					since "zsv 0.6"
				}
				column {
					name "rootVolumeUuid"
					enclosedIn "params"
					desc "根云盘UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "platform"
					enclosedIn "params"
					desc "根云盘镜像对应的系统平台"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
					values ("Linux","Windows","Other","Paravirtualization","WindowsVirtio")
				}
				column {
					name "system"
					enclosedIn "params"
					desc "是否系统根云盘镜像"
					location "body"
					type "boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "根云盘镜像UUID。若指定，根云盘镜像会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
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
			clz APICreateRootVolumeTemplateFromRootVolumeEvent.class
		}
	}
}