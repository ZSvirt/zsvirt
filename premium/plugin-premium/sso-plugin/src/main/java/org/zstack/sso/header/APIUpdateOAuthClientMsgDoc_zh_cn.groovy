package org.zstack.sso.header

import org.zstack.sso.header.APIUpdateOAuthClientEvent

doc {
	title "UpdateOAuthClient"

	category "sso"

	desc """更新 OAuth2 客户端"""

	rest {
		request {
			url "POST /v1/update/oauth2/client"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateOAuthClientMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "OAuth2 客户端的 UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "OAuth2 客户端名称"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "OAuth2 客户端的详细描述"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "clientId"
					enclosedIn "params"
					desc "客户端 ID"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "clientSecret"
					enclosedIn "params"
					desc "客户端密钥"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "authorizationUrl"
					enclosedIn "params"
					desc "认证 URL"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "tokenUrl"
					enclosedIn "params"
					desc "认证 Token URL"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "redirectUrl"
					enclosedIn "params"
					desc "用户自定义回调的 URL"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "userinfoUrl"
					enclosedIn "params"
					desc "用户信息 URL"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "logoutUrl"
					enclosedIn "params"
					desc "用户登出 URL"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "usernameProperty"
					enclosedIn "params"
					desc "用户登录该虚拟化平台时使用哪个字段用作用户名"
					location "body"
					type "String"
					optional true
					since "4.10.0"
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
			clz APIUpdateOAuthClientEvent.class
		}
	}
}