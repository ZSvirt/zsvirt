package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIGetVolumeSnapshotSizeEvent

doc {
	title "GetVolumeSnapshotSize"

	category "snapshot.volume"

	desc """获取快照容量"""

	rest {
		request {
			url "PUT /v1/volume-snapshots/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVolumeSnapshotSizeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "getVolumeSnapshotSize"
					desc "快照UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.5"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.5"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.5"
				}
			}
		}

		response {
			clz APIGetVolumeSnapshotSizeEvent.class
		}
	}
}