package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIGetRolePolicyActionsReply

doc {
	title "GetRolePolicyActions"

	category "identity"

	desc """获取角色权限行动"""

	rest {
		request {
			url "GET /v1/identities/role/policy-actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetRolePolicyActionsMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "showAllPolicies"
					enclosedIn ""
					desc "是否查询系统内所有角色权限。true 则查询查询系统内所有角色权限，false 仅查询当前账户拥有的角色和权限"
					location "query"
					type "boolean"
					optional true
					since "zsv 4.10.0"
				}
			}
		}

		response {
			clz APIGetRolePolicyActionsReply.class
		}
	}
}