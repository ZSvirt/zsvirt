package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIUpdateRoleEvent

doc {
	title "UpdateRole"

	category "rbac"

	desc """更新角色"""

	rest {
		request {
			url "PUT /v1/identities/roles/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateRoleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateRole"
					desc "角色的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "name"
					enclosedIn "updateRole"
					desc "角色名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "description"
					enclosedIn "updateRole"
					desc "角色的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "createPolicies"
					enclosedIn "updateRole"
					desc "为角色创建新的权限条目"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "clearPoliciesBeforeUpdate"
					enclosedIn "updateRole"
					desc "是否在更新角色之前清理掉所有的角色权限条目"
					location "body"
					type "boolean"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "deletePolicies"
					enclosedIn "updateRole"
					desc "删除角色已有的权限条目"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
			}
		}

		response {
			clz APIUpdateRoleEvent.class
		}
	}
}