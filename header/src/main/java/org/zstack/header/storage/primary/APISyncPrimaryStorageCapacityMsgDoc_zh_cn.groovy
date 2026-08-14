package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APISyncPrimaryStorageCapacityEvent

doc {
	title "刷新主存储容量(SyncPrimaryStorageCapacity)"

	category "storage.primary"

	desc """刷新主存储容量"""

	rest {
		request {
			url "PUT /v1/primary-storage/{primaryStorageUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncPrimaryStorageCapacityMsg.class

			desc """"""

			params {

				column {
					name "primaryStorageUuid"
					enclosedIn "syncPrimaryStorageCapacity"
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
			clz APISyncPrimaryStorageCapacityEvent.class
		}
	}
}