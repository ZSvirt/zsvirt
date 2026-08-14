package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIShrinkVolumeSnapshotEvent

doc {
	title "ShrinkVolumeSnapshot"

	category "snapshot.volume"

	desc """快照瘦身"""

	rest {
		request {
			url "PUT /v1/volume-snapshots/shrink/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIShrinkVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "shrinkVolumeSnapshot"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.10"
				}
			}
		}

		response {
			clz APIShrinkVolumeSnapshotEvent.class
		}
	}
}