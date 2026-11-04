package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APIGetVolumeSnapshotGroupTreeReply

doc {
	title "GetVolumeSnapshotGroupTree"

	category "snapshot.volume"

	desc """查询指定虚拟机的快照组树"""

	rest {
		request {
			url "GET /v1/volume-snapshots/group/trees"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVolumeSnapshotGroupTreeMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "虚拟机UUID"
					location "query"
					type "String"
					optional false
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv_5.2.0"
				}
			}
		}

		response {
			clz APIGetVolumeSnapshotGroupTreeReply.class
		}
	}
}