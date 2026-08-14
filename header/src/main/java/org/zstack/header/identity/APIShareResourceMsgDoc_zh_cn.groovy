package org.zstack.header.identity

import org.zstack.header.identity.APIShareResourceEvent

doc {
	title "ShareResource"

	category "identity"

	desc """共享资源给账户"""

	rest {
		request {
			url "PUT /v1/accounts/resources/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIShareResourceMsg.class

			desc """共享资源给账户"""

			params {

				column {
					name "resourceUuids"
					enclosedIn "shareResource"
					desc "资源UUID列表"
					location "body"
					type "List"
					optional false
					since "zsv 0.6"
				}
				column {
					name "accountUuids"
					enclosedIn "shareResource"
					desc "账户UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "toPublic"
					enclosedIn "shareResource"
					desc "全局共享"
					location "body"
					type "boolean"
					optional true
					since "zsv 0.6"
				}
				column {
					name "permission"
					enclosedIn "shareResource"
					desc "共享的权限, WRITE 表示写权限, READ 表示读权限"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
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
			clz APIShareResourceEvent.class
		}
	}
}