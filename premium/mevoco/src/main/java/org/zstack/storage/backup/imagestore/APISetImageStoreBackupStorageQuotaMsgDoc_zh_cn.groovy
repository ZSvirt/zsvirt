package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APISetImageStoreBackupStorageQuotaEvent

doc {
	title "SetImageStoreBackupStorageQuota"

	category "storage.backup.imagestore"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/backup-storage/image-store/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetImageStoreBackupStorageQuotaMsg.class

			desc """"""

			params {

				column {
					name "uuids"
					enclosedIn "setImageStoreBackupStorageQuota"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "maxCapacity"
					enclosedIn "setImageStoreBackupStorageQuota"
					desc ""
					location "body"
					type "long"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISetImageStoreBackupStorageQuotaEvent.class
		}
	}
}