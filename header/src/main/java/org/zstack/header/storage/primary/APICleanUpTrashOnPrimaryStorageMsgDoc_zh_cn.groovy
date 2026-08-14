package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APICleanUpTrashOnPrimaryStorageEvent

doc {
	title "CleanUpTrashOnPrimaryStorage"

	category "storage.primary"

	desc """清理主存储上的回收数据"""

	rest {
		request {
			url "PUT /v1/primary-storage/{uuid}/trash/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanUpTrashOnPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "cleanUpTrashOnPrimaryStorage"
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.2.0"
				}
				column {
					name "trashId"
					enclosedIn "cleanUpTrashOnPrimaryStorage"
					desc "单独清理的id"
					location "body"
					type "Long"
					optional true
					since "zsv 3.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.2.0"
				}
			}
		}

		response {
			clz APICleanUpTrashOnPrimaryStorageEvent.class
		}
	}
}