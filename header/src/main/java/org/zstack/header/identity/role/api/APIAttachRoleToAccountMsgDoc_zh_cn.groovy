package org.zstack.header.identity.role.api

import org.zstack.header.identity.role.api.APIAttachRoleToAccountEvent

doc {
	title "AttachRoleToAccount"

	category "rbac"

	desc """绑定角色和账户"""

	rest {
		request {
			url "POST /v1/identities/accounts/{accountUuid}/roles/{roleUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachRoleToAccountMsg.class

			desc """"""

			params {

				column {
					name "roleUuid"
					enclosedIn "params"
					desc "角色 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.0"
				}
			}
		}

		response {
			clz APIAttachRoleToAccountEvent.class
		}
	}
}