package org.zstack.header.image

import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeSnapshotEvent

doc {
	title "从云盘快照创建数据云盘镜像(CreateDataVolumeTemplateFromVolumeSnapshot)"

	category "image"

	desc """从指定的云盘快照创建出一个数据云盘镜像"""

	rest {
		request {
			url "POST /v1/images/data-volume-templates/from/volume-snapshots/{snapshotUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateDataVolumeTemplateFromVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "snapshotUuid"
					enclosedIn "params"
					desc "数据云盘快照的资源UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "数据云盘镜像名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "数据云盘镜像描述"
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
					name "resourceUuid"
					enclosedIn "params"
					desc "数据云盘镜像UUID。若指定，数据云盘镜像会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
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
			}
		}

		response {
			clz APICreateDataVolumeTemplateFromVolumeSnapshotEvent.class
		}
	}
}