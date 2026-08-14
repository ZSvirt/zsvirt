package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIGetPrimaryStorageCapacityReply

doc {
	title "获取主存储容量(GetPrimaryStorageCapacity)"

	category "storage.primary"

	desc """获取主存储容量"""

	rest {
		request {
			url "GET /v1/primary-storage/capacities"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPrimaryStorageCapacityMsg.class

			desc """"""

			params {

				column {
					name "zoneUuids"
					enclosedIn ""
					desc "区域UUID列表"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "clusterUuids"
					enclosedIn ""
					desc "集群UUID列表"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "primaryStorageUuids"
					enclosedIn ""
					desc "主存储UUID列表"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "all"
					enclosedIn ""
					desc "当主存储UUID列表为空时，该项为真表示查询系统中所有的主存储。"
					location "query"
					type "boolean"
					optional true
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
			clz APIGetPrimaryStorageCapacityReply.class
		}
	}
}