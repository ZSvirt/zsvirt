package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APIQueryVolumeSnapshotGroupReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVolumeSnapshotGroup"

	category "snapshot.volume"

	desc """查询快照组"""

	rest {
		request {
			url "GET /v1/volume-snapshots/group"
			url "GET /v1/volume-snapshots/group/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVolumeSnapshotGroupMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVolumeSnapshotGroupReply.class
		}
	}
}