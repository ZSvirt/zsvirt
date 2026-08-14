package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIDiscoverStrangePrimaryStorageReply

doc {
	title "DiscoverStrangePrimaryStorage"

	category "storage.primary"

	desc """发现集群物理机上未受当前平台管控的主存储"""

	rest {
		request {
			url "GET /v1/primary-storage/stranger"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDiscoverStrangePrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "String"
					optional false
					since "zsv 5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APIDiscoverStrangePrimaryStorageReply.class
		}
	}
}