package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIDetachRoleFromAccountGroupEvent

doc {
	title "DetachRoleFromAccountGroup"

	category "iam1Accounts"

	desc """账户组解绑角色"""

	rest {
		request {
			url "DELETE /v1/account-groups/{groupUuid}/roles"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachRoleFromAccountGroupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn ""
					desc "账户组 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "roleUuids"
					enclosedIn ""
					desc "角色 UUID 列表"
					location "query"
					type "List"
					optional false
					since "4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIDetachRoleFromAccountGroupEvent.class
		}
	}
}