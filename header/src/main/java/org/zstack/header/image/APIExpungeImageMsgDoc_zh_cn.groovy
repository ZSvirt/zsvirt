package org.zstack.header.image

import org.zstack.header.image.APIExpungeImageEvent

doc {
	title "彻底删除镜像(ExpungeImage)"

	category "image"

	desc """彻底删除镜像"""

	rest {
		request {
			url "PUT /v1/images/{imageUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExpungeImageMsg.class

			desc """"""

			params {

				column {
					name "imageUuid"
					enclosedIn "expungeImage"
					desc "镜像UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "backupStorageUuids"
					enclosedIn "expungeImage"
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
				column {
					name "uuid"
					enclosedIn "expungeImage"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional true
					since "zsv 3.2.0"
				}
			}
		}

		response {
			clz APIExpungeImageEvent.class
		}
	}
}