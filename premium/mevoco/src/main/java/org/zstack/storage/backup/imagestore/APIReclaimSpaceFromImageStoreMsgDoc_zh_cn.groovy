package org.zstack.storage.backup.imagestore

import org.zstack.storage.backup.imagestore.APIReclaimSpaceFromImageStoreEvent

doc {
	title "从镜像仓库回收磁盘空间(ReclaimSpaceFromImageStore)"

	category "storage.backup.imagestore"

	desc """从镜像仓库释放无需再使用的磁盘空间"""

	rest {
		request {
			url "PUT /v1/backup-storage/image-store/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReclaimSpaceFromImageStoreMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "reclaimSpaceFromImageStore"
					desc "镜像仓库的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "1.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.10"
				}
			}
		}

		response {
			clz APIReclaimSpaceFromImageStoreEvent.class
		}
	}
}