package org.zstack.header.volume

import org.zstack.header.volume.APICreateVolumeSnapshotGroupEvent

doc {
	title "CreateVolumeSnapshotGroup"

	category "volume"

	desc """创建云盘快照组"""

	rest {
		request {
			url "POST /v1/volume-snapshots/group"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVolumeSnapshotGroupMsg.class

			desc """"""

			params {

				column {
					name "rootVolumeUuid"
					enclosedIn "params"
					desc "根云盘UUID"
					location "body"
					type "String"
					optional false
					since "zsv 3.6.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 3.6.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "withMemory"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "zsv 3.8.0"
				}
			}
		}

		response {
			clz APICreateVolumeSnapshotGroupEvent.class
		}
	}
}