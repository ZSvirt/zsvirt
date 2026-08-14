package org.zstack.storage.primary.local

import org.zstack.storage.primary.local.APIGetLocalStorageHostDiskCapacityReply

doc {
	title "获取主机本地存储容量(GetLocalStorageHostDiskCapacity)"

	category "storage.primary"

	desc """获取本地存储中物理机本地盘磁盘容量"""

	rest {
		request {
			url "GET /v1/primary-storage/local-storage/{primaryStorageUuid}/capacities"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetLocalStorageHostDiskCapacityMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "primaryStorageUuid"
					enclosedIn ""
					desc "主存储UUID"
					location "url"
					type "String"
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
			clz APIGetLocalStorageHostDiskCapacityReply.class
		}
	}
}