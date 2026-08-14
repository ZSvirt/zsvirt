package org.zstack.header.storage.backup

import org.zstack.header.storage.backup.APICleanUpTrashOnBackupStorageEvent

doc {
	title "CleanUpTrashOnBackupStorage"

	category "storage.backup"

	desc """清理备份存储上的回收数据"""

	rest {
		request {
			url "PUT /v1/backup-storage/{uuid}/trash/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanUpTrashOnBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "cleanUpTrashOnBackupStorage"
					desc "备份存储UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.2.0"
				}
				column {
					name "trashId"
					enclosedIn "cleanUpTrashOnBackupStorage"
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
			clz APICleanUpTrashOnBackupStorageEvent.class
		}
	}
}