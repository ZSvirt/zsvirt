package org.zstack.header.volume

import org.zstack.header.volume.APICreateDataVolumeFromVolumeTemplateEvent

doc {
	title "从镜像创建云盘(CreateDataVolumeFromVolumeTemplate)"

	category "volume"

	desc """从镜像创建云盘"""

	rest {
		request {
			url "POST /v1/volumes/data/from/data-volume-templates/{imageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateDataVolumeFromVolumeTemplateMsg.class

			desc """"""

			params {

				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "云盘名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "云盘的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
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
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
			}
		}

		response {
			clz APICreateDataVolumeFromVolumeTemplateEvent.class
		}
	}
}