package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIDetachPrimaryStorageFromClusterEvent

doc {
	title "从集群卸载主存储(DetachPrimaryStorageFromCluster)"

	category "storage.primary"

	desc """从集群卸载主存储"""

	rest {
		request {
			url "DELETE /v1/clusters/{clusterUuid}/primary-storage/{primaryStorageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachPrimaryStorageFromClusterMsg.class

			desc """"""

			params {

				column {
					name "primaryStorageUuid"
					enclosedIn ""
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDetachPrimaryStorageFromClusterEvent.class
		}
	}
}