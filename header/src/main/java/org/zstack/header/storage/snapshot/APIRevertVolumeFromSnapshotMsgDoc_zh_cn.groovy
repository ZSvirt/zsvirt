package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIRevertVolumeFromSnapshotEvent

doc {
	title "将云盘回滚至指定快照(RevertVolumeFromSnapshot)"

	category "snapshot.volume"

	desc """将云盘回滚至某个指定的快照"""

	rest {
		request {
			url "PUT /v1/volume-snapshots/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRevertVolumeFromSnapshotMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "revertVolumeFromSnapshot"
					desc "快照的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
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
			clz APIRevertVolumeFromSnapshotEvent.class
		}
	}
}