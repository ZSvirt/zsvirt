package org.zstack.sso.header

import org.zstack.sso.header.APICreateOAuthClientEvent

doc {
	title "CreateOAuthClient"

	category "sso"

	desc """创建 OAuth2 客户端"""

	rest {
		request {
			url "POST /v1/create/oauth2/client"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateOAuthClientMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
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
					optional false
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
					optional false
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
					name "redirectUrl"
					enclosedIn "params"
					desc "用户自定义回调 URL"
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
					name "grantType"
					enclosedIn "params"
					desc "认证模式"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "urlTemplate"
					enclosedIn "params"
					desc "免密登录之后的跳转的模板"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "clientType"
					enclosedIn "params"
					desc "客户端类型"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源 UUID"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签 UUID 列表"
					location "body"
					type "List"
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
			clz APICreateOAuthClientEvent.class
		}
	}
}