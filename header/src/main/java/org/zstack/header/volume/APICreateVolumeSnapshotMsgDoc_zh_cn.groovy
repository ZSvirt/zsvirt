package org.zstack.header.volume

import org.zstack.header.volume.APICreateVolumeSnapshotEvent

doc {
	title "CreateVolumeSnapshot"

	category "volume"

	desc """创建云盘快照"""

	rest {
		request {
			url "POST /v1/volumes/{volumeUuid}/volume-snapshots"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "volumeUuid"
					enclosedIn "params"
					desc "云盘UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
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
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
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
			clz APICreateVolumeSnapshotEvent.class
		}
	}
}