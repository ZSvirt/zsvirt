package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIRemoveMonFromCephPrimaryStorageEvent

doc {
	title "从 Ceph 主存储删除 mon 节点(RemoveMonFromCephPrimaryStorage)"

	category "storage.ceph.primary"

	desc """从 Ceph 主存储删除 mon 节点"""

	rest {
		request {
			url "DELETE /v1/primary-storage/ceph/{uuid}/mons"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveMonFromCephPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "Ceph 主存储的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "monHostnames"
					enclosedIn ""
					desc "mon 节点名字列表"
					location "query"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIRemoveMonFromCephPrimaryStorageEvent.class
		}
	}
}