package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIUpdateVolumeSnapshotEvent

doc {
	title "更新云盘快照信息(UpdateVolumeSnapshot)"

	category "snapshot.volume"

	desc """更新云盘快照信息"""

	rest {
		request {
			url "PUT /v1/volume-snapshots/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVolumeSnapshot"
					desc "快照的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateVolumeSnapshot"
					desc "快照的新名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updateVolumeSnapshot"
					desc "快照的新详细描述"
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
			}
		}

		response {
			clz APIUpdateVolumeSnapshotEvent.class
		}
	}
}