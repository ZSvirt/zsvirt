package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APISyncImageFromImageStoreBackupStorageEvent

doc {
	title "SyncImageFromImageStoreBackupStorage"

	category "storage.backup.imagestore"

	desc """将镜像同步到灾备服务器"""

	rest {
		request {
			url "PUT /v1/images/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISyncImageFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "syncImageFromImageStoreBackupStorage"
					desc "要备份镜像的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "srcBackupStorageUuid"
					enclosedIn "syncImageFromImageStoreBackupStorage"
					desc "源镜像服务器UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "dstBackupStorageUuid"
					enclosedIn "syncImageFromImageStoreBackupStorage"
					desc "灾备服务器UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "name"
					enclosedIn "syncImageFromImageStoreBackupStorage"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "description"
					enclosedIn "syncImageFromImageStoreBackupStorage"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APISyncImageFromImageStoreBackupStorageEvent.class
		}
	}
}