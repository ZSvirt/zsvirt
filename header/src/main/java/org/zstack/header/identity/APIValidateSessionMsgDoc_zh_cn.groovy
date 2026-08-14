package org.zstack.header.identity

import org.zstack.header.identity.APIValidateSessionReply

doc {
	title "ValidateSession"

	category "identity"

	desc """验证会话的有效性"""

	rest {
		request {
			url "GET /v1/accounts/sessions/{sessionUuid}/valid"



			clz APIValidateSessionMsg.class

			desc """验证会话的有效性"""

			params {

				column {
					name "sessionUuid"
					enclosedIn ""
					desc "会话UUID"
					location "url"
					type "String"
					optional false
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
			clz APIValidateSessionReply.class
		}
	}
}