package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.APICheckMemorySnapshotGroupConflictReply

doc {
	title "CheckMemorySnapshotGroupConflict"

	category "vmInstance"

	desc """检查内存快照组冲突信息"""

	rest {
		request {
			url "GET /v1/memory-snapshots/groups/{uuid}/conflict-detection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckMemorySnapshotGroupConflictMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "内存快照组UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APICheckMemorySnapshotGroupConflictReply.class
		}
	}
}