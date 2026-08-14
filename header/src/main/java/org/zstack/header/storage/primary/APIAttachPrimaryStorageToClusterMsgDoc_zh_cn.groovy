package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIAttachPrimaryStorageToClusterEvent

doc {
	title "向集群添加主存储(AttachPrimaryStorageToCluster)"

	category "storage.primary"

	desc """向集群添加主存储"""

	rest {
		request {
			url "POST /v1/clusters/{clusterUuid}/primary-storage/{primaryStorageUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachPrimaryStorageToClusterMsg.class

			desc """"""

			params {

				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIAttachPrimaryStorageToClusterEvent.class
		}
	}
}