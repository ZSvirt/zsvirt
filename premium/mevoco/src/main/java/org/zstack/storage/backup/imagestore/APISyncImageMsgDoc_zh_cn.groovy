package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APISyncImageEvent

doc {
	title "SyncImage"

	category "storage.backup.imagestore"

	desc """从镜像服务器元数据中同步镜像"""

	rest {
		request {
			url "PUT /v1/backup-storage/image-store/{imageStoreUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncImageMsg.class

			desc """从镜像服务器元数据中同步镜像"""

			params {

				column {
					name "imageStoreUuid"
					enclosedIn "syncImage"
					desc "镜像服务UUID"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APISyncImageEvent.class
		}
	}
}