package org.zstack.sso.header

import org.zstack.sso.header.APIDeleteSSOClientEvent

doc {
	title "DeleteSSOClient"

	category "sso"

	desc """删除认证客户端"""

	rest {
		request {
			url "POST /v1/delete/sso/client"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteSSOClientMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "SSO 客户端 UUID，可以是 OAuth2 或 CAS 的客户端 UUID"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "deleteMode"
					enclosedIn "params"
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
			}
		}

		response {
			clz APIDeleteSSOClientEvent.class
		}
	}
}