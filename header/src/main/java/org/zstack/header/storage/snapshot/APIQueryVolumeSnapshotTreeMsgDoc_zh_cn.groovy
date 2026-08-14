package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotTreeReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询快照树(QueryVolumeSnapshotTree)"

	category "snapshot.volume"

	desc """查询快照树"""

	rest {
		request {
			url "GET /v1/volume-snapshots/trees"
			url "GET /v1/volume-snapshots/trees/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVolumeSnapshotTreeMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVolumeSnapshotTreeReply.class
		}
	}
}