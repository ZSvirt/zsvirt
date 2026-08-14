package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APIUpdateVolumeSnapshotGroupEvent

doc {
	title "UpdateVolumeSnapshotGroup"

	category "snapshot.volume"

	desc """更新云盘快照组信息"""

	rest {
		request {
			url "PUT /v1/volume-snapshots/group/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVolumeSnapshotGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "updateVolumeSnapshotGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "description"
					enclosedIn "updateVolumeSnapshotGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "uuid"
					enclosedIn "updateVolumeSnapshotGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
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
			}
		}

		response {
			clz APIUpdateVolumeSnapshotGroupEvent.class
		}
	}
}