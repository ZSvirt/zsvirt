package org.zstack.header.image

import org.zstack.header.image.APISetImageBootModeEvent

doc {
	title "SetImageBootMode"

	category "image"

	desc """设置镜像启动模式"""

	rest {
		request {
			url "PUT /v1/images/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetImageBootModeMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setImageBootMode"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 3.9.0"
				}
				column {
					name "bootMode"
					enclosedIn "setImageBootMode"
					desc "镜像启动模式"
					location "body"
					type "String"
					optional false
					since "zsv 3.9.0"
					values ("Legacy","UEFI","UEFI_WITH_CSM")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.9.0"
				}
			}
		}

		response {
			clz APISetImageBootModeEvent.class
		}
	}
}