package org.zstack.sso.header

import org.zstack.sso.header.APIGetOAuthClientSecretReply

doc {
	title "获取 OAuth2 Client Secret"

	category "sso"

	desc """获取 OAuth2 客户端的 Client Secret"""

	rest {
		request {
			url "GET /v1/oauth2/clients/{uuid}/client-secret"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetOAuthClientSecretMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "OAuth2 客户端 UUID"
					location "url"
					type "String"
					optional false
					since "5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APIGetOAuthClientSecretReply.class
		}
	}
}