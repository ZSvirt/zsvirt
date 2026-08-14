package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APIReconnectImageStoreBackupStorageEvent

doc {
	title "重连镜像仓库服务器(ReconnectImageStoreBackupStorage)"

	category "storage.backup.imagestore"

	desc """重连镜像仓库服务器"""

	rest {
		request {
			url "PUT /v1/backup-storage/image-store/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReconnectImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "reconnectImageStoreBackupStorage"
					desc "镜像仓库服务器的UUID"
					location "url"
					type "String"
					optional false
					since "1.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.6"
				}
			}
		}

		response {
			clz APIReconnectImageStoreBackupStorageEvent.class
		}
	}
}