package org.zstack.header.identity

import org.zstack.header.identity.APIChangeAccountTypeEvent

doc {
	title "ChangeAccountType"

	category "identity"

	desc """变更账户类型（提权/降权）"""

	rest {
		request {
			url "PUT /v1/accounts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAccountTypeMsg.class

			desc """变更账户类型，支持将普通用户提升为管理员，或将管理员降级为普通用户（暂不支持）"""

			params {

				column {
					name "uuid"
					enclosedIn "changeAccountType"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 5.0.0"
				}
				column {
					name "type"
					enclosedIn "changeAccountType"
					desc "账户类型，SystemAdmin表示管理员，Normal表示普通用户（Normal暂不支持，即暂不支持降权）"
					location "body"
					type "String"
					optional false
					since "zsv 5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APIChangeAccountTypeEvent.class
		}
	}
}