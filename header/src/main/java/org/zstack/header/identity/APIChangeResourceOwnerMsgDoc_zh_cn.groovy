package org.zstack.header.identity

import org.zstack.header.identity.APIChangeResourceOwnerEvent

doc {
	title "ChangeResourceOwner"

	category "identity"

	desc """变更资源所有者"""

	rest {
		request {
			url "POST /v1/account/{accountUuid}/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeResourceOwnerMsg.class

			desc """变更资源所有者"""

			params {

				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
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
			clz APIChangeResourceOwnerEvent.class
		}
	}
}