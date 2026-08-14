package org.zstack.header.vm

import org.zstack.header.vm.APIGetMemorySnapshotGroupReferenceReply

doc {
	title "GetMemorySnapshotGroupReference"

	category "snapshot.volume"

	desc """获取资源被引用的内存快照组"""

	rest {
		request {
			url "GET /v1/memory-snapshots/group/reference"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetMemorySnapshotGroupReferenceMsg.class

			desc """"""

			params {

				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
					location "query"
					type "String"
					optional false
					since "zsv 3.14.24"
				}
				column {
					name "resourceType"
					enclosedIn ""
					desc "资源类型"
					location "query"
					type "String"
					optional false
					since "zsv 3.14.24"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.14.24"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 3.14.24"
				}
			}
		}

		response {
			clz APIGetMemorySnapshotGroupReferenceReply.class
		}
	}
}