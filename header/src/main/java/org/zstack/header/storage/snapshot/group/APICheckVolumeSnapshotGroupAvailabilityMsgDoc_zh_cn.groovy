package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APICheckVolumeSnapshotGroupAvailabilityReply

doc {
	title "CheckVolumeSnapshotGroupAvailability"

	category "snapshot.volume"

	desc """检查快照组可用性"""

	rest {
		request {
			url "GET /v1/volume-snapshots/groups/availabilities"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckVolumeSnapshotGroupAvailabilityMsg.class

			desc """"""

			params {

				column {
					name "uuids"
					enclosedIn ""
					desc "快照组UUIDs"
					location "query"
					type "List"
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
			clz APICheckVolumeSnapshotGroupAvailabilityReply.class
		}
	}
}