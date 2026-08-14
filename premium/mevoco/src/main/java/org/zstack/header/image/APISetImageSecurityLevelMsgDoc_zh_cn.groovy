package org.zstack.header.image

import org.zstack.header.image.APISetImageSecurityLevelEvent

doc {
	title "SetImageSecurityLevel"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/images/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetImageSecurityLevelMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setImageSecurityLevel"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "securityLevel"
					enclosedIn "setImageSecurityLevel"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISetImageSecurityLevelEvent.class
		}
	}
}