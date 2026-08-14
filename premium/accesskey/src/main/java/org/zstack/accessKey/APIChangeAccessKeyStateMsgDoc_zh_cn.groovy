package org.zstack.accessKey

import org.zstack.accessKey.APIChangeAccessKeyStateEvent

doc {
	title "ChangeAccessKeyState"

	category "accessKey"

	desc """开启或关闭AccessKey"""

	rest {
		request {
			url "PUT /v1/accesskeys/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAccessKeyStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeAccessKeyState"
					desc "AccessKey UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "stateEvent"
					enclosedIn "changeAccessKeyState"
					desc "开启或关闭"
					location "body"
					type "String"
					optional false
					since "4.0.0"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
			}
		}

		response {
			clz APIChangeAccessKeyStateEvent.class
		}
	}
}