package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询云盘快照(QueryVolumeSnapshot)"

	category "snapshot.volume"

	desc """查询云盘快照"""

	rest {
		request {
			url "GET /v1/volume-snapshots"
			url "GET /v1/volume-snapshots/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVolumeSnapshotMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVolumeSnapshotReply.class
		}
	}
}