package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIGetPrimaryStorageAllocatorStrategiesReply

doc {
	title "获取主存储分配策略清单(GetPrimaryStorageAllocatorStrategies)"

	category "storage.primary"

	desc """获取主存储分配策略清单"""

	rest {
		request {
			url "GET /v1/primary-storage/allocators/strategies"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPrimaryStorageAllocatorStrategiesMsg.class

			desc """"""

			params {

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
			clz APIGetPrimaryStorageAllocatorStrategiesReply.class
		}
	}
}