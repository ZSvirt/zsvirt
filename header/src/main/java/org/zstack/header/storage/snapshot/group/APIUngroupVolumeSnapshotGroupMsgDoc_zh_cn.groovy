package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APIUngroupVolumeSnapshotGroupEvent

doc {
	title "UngroupVolumeSnapshotGroup"

	category "snapshot.volume"

	desc """解绑快照组"""

	rest {
		request {
			url "DELETE /v1/volume-snapshots/ungroup/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUngroupVolumeSnapshotGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "快照组的UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 3.6.0"
				}
			}
		}

		response {
			clz APIUngroupVolumeSnapshotGroupEvent.class
		}
	}
}