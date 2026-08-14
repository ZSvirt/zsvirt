package org.zstack.header.identity

import org.zstack.header.identity.APIRenewSessionEvent

doc {
	title "RenewSession"

	category "identity"

	desc """更新会话"""

	rest {
		request {
			url "PUT /v1/accounts/sessions/{sessionUuid}/renew"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRenewSessionMsg.class

			desc """"""

			params {

				column {
					name "sessionUuid"
					enclosedIn "renewSession"
					desc ""
					location "url"
					type "String"
					optional false
					since "zsv 2.3"
				}
				column {
					name "duration"
					enclosedIn "renewSession"
					desc ""
					location "body"
					type "Long"
					optional true
					since "zsv 2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 2.3"
				}
			}
		}

		response {
			clz APIRenewSessionEvent.class
		}
	}
}