package org.zstack.sso.header

import org.zstack.sso.header.APICreateSSORedirectTemplateEvent

doc {
	title "CreateSSORedirectTemplate"

	category "sso"

	desc """创建认证成功跳转的模版"""

	rest {
		request {
			url "POST /v1/create/sso/redirect/template/"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSSORedirectTemplateMsg.class

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
					optional false
					since "4.3.0"
				}
				column {
					name "clientUuid"
					enclosedIn "params"
					desc "对应的 SSO 客户端 UUID，即第三方账户源的 UUID"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "redirectTemplate"
					enclosedIn "params"
					desc "跳转的模版"
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
			clz APICreateSSORedirectTemplateEvent.class
		}
	}
}