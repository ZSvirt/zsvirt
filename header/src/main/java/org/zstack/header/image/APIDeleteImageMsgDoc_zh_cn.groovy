package org.zstack.header.image

import org.zstack.header.image.APIDeleteImageEvent

doc {
	title "删除镜像(DeleteImage)"

	category "image"

	desc """删除镜像"""

	rest {
		request {
			url "DELETE /v1/images/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteImageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "镜像的UUID，唯一标示该镜像"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "backupStorageUuids"
					enclosedIn ""
					desc "镜像服务器UUID列表"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive 或者 Enforcing, 默认 Permissive)"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIDeleteImageEvent.class
		}
	}
}