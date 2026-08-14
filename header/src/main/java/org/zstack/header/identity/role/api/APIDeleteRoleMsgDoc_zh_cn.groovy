package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIDeleteRoleEvent

doc {
	title "DeleteRole"

	category "rbac"

	desc """删除角色"""

	rest {
		request {
			url "DELETE /v1/identities/roles/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteRoleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "角色的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式。'Permissive' 是删除前检查模式, 如果这个角色还绑定着账户, 删除会失败; 'Enforcing' 是强制删除模式, 如果这个角色还绑定着账户，哪些账户自动解绑角色"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
			}
		}

		response {
			clz APIDeleteRoleEvent.class
		}
	}
}