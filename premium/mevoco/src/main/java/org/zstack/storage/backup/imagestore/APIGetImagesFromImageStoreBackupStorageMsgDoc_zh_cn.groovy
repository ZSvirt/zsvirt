package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APIGetImagesFromImageStoreBackupStorageReply

doc {
	title "ListImagesFromImageStoreBackupStorage"

	category "storage.backup.imagestore"

	desc """获取镜像仓库中的原始数据"""

	rest {
		request {
			url "PUT /v1/backup-storage/{uuid}/image-store"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetImagesFromImageStoreBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "getImagesFromImageStoreBackupStorage"
					desc "镜像仓库UUID"
					location "url"
					type "String"
					optional false
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
			clz APIGetImagesFromImageStoreBackupStorageReply.class
		}
	}
}