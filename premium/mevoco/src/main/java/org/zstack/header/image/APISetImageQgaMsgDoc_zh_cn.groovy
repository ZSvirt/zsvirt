package org.zstack.header.image

import org.zstack.header.image.APISetImageQgaEvent

doc {
	title "SetImageQga"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/images/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetImageQgaMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setImageQga"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "enable"
					enclosedIn "setImageQga"
					desc ""
					location "body"
					type "boolean"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APISetImageQgaEvent.class
		}
	}
}