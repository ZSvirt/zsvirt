package org.zstack.header.storage.snapshot

import org.zstack.header.storage.snapshot.APIBatchDeleteVolumeSnapshotEvent

doc {
	title "BatchDeleteVolumeSnapshot"

	category "snapshot.volume"

	desc """批量删除云盘快照"""

	rest {
		request {
			url "PUT /v1/volume-snapshots/batch-delete"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBatchDeleteVolumeSnapshotMsg.class

			desc """"""

			params {

				column {
					name "uuids"
					enclosedIn "batchDeleteVolumeSnapshot"
					desc "云盘快照UUID列表"
					location "body"
					type "List"
					optional false
					since "zsv 3.3"
				}
				column {
					name "deleteMode"
					enclosedIn "batchDeleteVolumeSnapshot"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 3.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
			}
		}

		response {
			clz APIBatchDeleteVolumeSnapshotEvent.class
		}
	}
}