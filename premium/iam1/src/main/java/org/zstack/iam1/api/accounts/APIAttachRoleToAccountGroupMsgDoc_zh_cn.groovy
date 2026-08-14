package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIAttachRoleToAccountGroupEvent

doc {
	title "AttachRoleToAccountGroup"

	category "iam1Accounts"

	desc """账户组绑定角色"""

	rest {
		request {
			url "POST /v1/account-groups/{groupUuid}/roles"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachRoleToAccountGroupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn "params"
					desc "账户组 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "roleUuids"
					enclosedIn "params"
					desc "角色 UUID 列表"
					location "body"
					type "List"
					optional false
					since "4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIAttachRoleToAccountGroupEvent.class
		}
	}
}