package org.zstack.header.image

import org.zstack.header.image.APIChangeImageStateEvent

doc {
	title "修改镜像状态(ChangeImageState)"

	category "image"

	desc """修改镜像状态"""

	rest {
		request {
			url "PUT /v1/images/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeImageStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeImageState"
					desc "镜像的UUID，唯一标示该镜像"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeImageState"
					desc "镜像的状态"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
					values ("enable","disable")
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
			clz APIChangeImageStateEvent.class
		}
	}
}