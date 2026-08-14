package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APICleanUpStorageTrashOnPrimaryStorageEvent

doc {
	title "清空主存储垃圾(CleanUpStorageTrashOnPrimaryStorage)"

	category "storage.primary"

	desc """清空主存储垃圾"""

	rest {
		request {
			url "PUT /v1/primary-storage/{uuid}/storagetrash/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanUpStorageTrashOnPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "cleanUpStorageTrashOnPrimaryStorage"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.18.0"
				}
				column {
					name "force"
					enclosedIn "cleanUpStorageTrashOnPrimaryStorage"
					desc "是否强制清理主存储垃圾"
					location "body"
					type "boolean"
					optional true
					since "zsv 3.18.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.18.0"
				}
			}
		}

		response {
			clz APICleanUpStorageTrashOnPrimaryStorageEvent.class
		}
	}
}