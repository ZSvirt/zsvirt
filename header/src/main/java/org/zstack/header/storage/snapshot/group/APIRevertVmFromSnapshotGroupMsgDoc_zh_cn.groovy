package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APIRevertVmFromSnapshotGroupEvent

doc {
	title "RevertVmFromSnapshotGroup"

	category "snapshot.volume"

	desc """从快照组恢复云主机"""

	rest {
		request {
			url "PUT /v1/volume-snapshots/group/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRevertVmFromSnapshotGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "revertVmFromSnapshotGroup"
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
					location "body"
					type "List"
					optional true
					since "zsv 3.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 3.6.0"
				}
			}
		}

		response {
			clz APIRevertVmFromSnapshotGroupEvent.class
		}
	}
}