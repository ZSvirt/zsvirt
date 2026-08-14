package org.zstack.ipsec

import org.zstack.ipsec.APIChangeIPSecConnectionStateEvent

doc {
	title "ChangeIPSecConnectionState"

	category "ipsec"

	desc """改变IPsec连接的使用状态"""

	rest {
		request {
			url "PUT /v1/ipsec/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeIPSecConnectionStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeIPSecConnectionState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeIPSecConnectionState"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
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
			clz APIChangeIPSecConnectionStateEvent.class
		}
	}
}