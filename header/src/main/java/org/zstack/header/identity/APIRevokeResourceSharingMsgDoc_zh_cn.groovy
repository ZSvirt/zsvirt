package org.zstack.header.identity

import org.zstack.header.identity.APIRevokeResourceSharingEvent

doc {
	title "RevokeResourceSharing"

	category "identity"

	desc """解除资源共享"""

	rest {
		request {
			url "PUT /v1/accounts/resources/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRevokeResourceSharingMsg.class

			desc """解除资源共享"""

			params {

				column {
					name "resourceUuids"
					enclosedIn "revokeResourceSharing"
					desc "资源UUID列表"
					location "body"
					type "List"
					optional false
					since "zsv 0.6"
				}
				column {
					name "toPublic"
					enclosedIn "revokeResourceSharing"
					desc "全局共享"
					location "body"
					type "boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "accountUuids"
					enclosedIn "revokeResourceSharing"
					desc "账户UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "all"
					enclosedIn "revokeResourceSharing"
					desc ""
					location "body"
					type "boolean"
					optional true
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
			clz APIRevokeResourceSharingEvent.class
		}
	}
}