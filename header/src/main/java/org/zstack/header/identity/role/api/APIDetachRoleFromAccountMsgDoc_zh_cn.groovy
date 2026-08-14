package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIDetachRoleFromAccountEvent

doc {
	title "DetachRoleFromAccount"

	category "rbac"

	desc """解绑角色和账户"""

	rest {
		request {
			url "DELETE /v1/identities/accounts/{accountUuid}/roles/{roleUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachRoleFromAccountMsg.class

			desc """"""

			params {

				column {
					name "roleUuid"
					enclosedIn ""
					desc "角色 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "accountUuid"
					enclosedIn ""
					desc "账户 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式, 当前 API 该字段无效"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.0"
				}
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
			}
		}

		response {
			clz APIDetachRoleFromAccountEvent.class
		}
	}
}