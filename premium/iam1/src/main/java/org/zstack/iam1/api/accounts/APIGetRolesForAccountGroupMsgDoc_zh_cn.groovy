package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIGetRolesForAccountGroupReply

doc {
	title "GetRolesForAccountGroup"

	category "iam1Accounts"

	desc """获取账户组绑定的角色"""

	rest {
		request {
			url "GET /v1/account-groups/{groupUuid}/roles"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetRolesForAccountGroupMsg.class

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
					name "includeInheritedRoles"
					enclosedIn ""
					desc "是否包含父账户组的继承的角色; 如果为 true, 则结果将包含父账户组继承的角色"
					location "query"
					type "boolean"
					optional true
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
			clz APIGetRolesForAccountGroupReply.class
		}
	}
}