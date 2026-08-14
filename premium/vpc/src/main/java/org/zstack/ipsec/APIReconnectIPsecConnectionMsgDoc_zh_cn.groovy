package org.zstack.ipsec

import org.zstack.ipsec.APIReconnectIPsecConnectionEvent

doc {
	title "ReconnectIPsecConnection"

	category "ipsec"

	desc """重连IPSec"""

	rest {
		request {
			url "PUT /v1/ipsec/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIReconnectIPsecConnectionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "reconnectIPsecConnection"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.15"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.15"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.15"
				}
			}
		}

		response {
			clz APIReconnectIPsecConnectionEvent.class
		}
	}
}