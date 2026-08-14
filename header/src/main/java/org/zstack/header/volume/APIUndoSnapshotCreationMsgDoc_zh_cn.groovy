package org.zstack.header.volume

import org.zstack.header.volume.APIUndoSnapshotCreationEvent

doc {
	title "UndoSnapshotCreation"

	category "volume"

	desc """撤销快照创建"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUndoSnapshotCreationMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "undoSnapshotCreation"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "snapShotUuid"
					enclosedIn "undoSnapshotCreation"
					desc ""
					location "body"
					type "String"
					optional false
					since "zsv 3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.17.0"
				}
			}
		}

		response {
			clz APIUndoSnapshotCreationEvent.class
		}
	}
}