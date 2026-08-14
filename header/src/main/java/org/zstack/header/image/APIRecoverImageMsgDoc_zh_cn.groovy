package org.zstack.header.image

import org.zstack.header.image.APIRecoverImageEvent

doc {
	title "恢复镜像(RecoverImage)"

	category "image"

	desc """恢复被删除（但未彻底删除）的镜像"""

	rest {
		request {
			url "PUT /v1/images/{imageUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRecoverImageMsg.class

			desc """"""

			params {

				column {
					name "imageUuid"
					enclosedIn "recoverImage"
					desc "镜像UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "backupStorageUuids"
					enclosedIn "recoverImage"
					desc "镜像服务器UUID列表"
					location "body"
					type "List"
					optional true
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
			clz APIRecoverImageEvent.class
		}
	}
}