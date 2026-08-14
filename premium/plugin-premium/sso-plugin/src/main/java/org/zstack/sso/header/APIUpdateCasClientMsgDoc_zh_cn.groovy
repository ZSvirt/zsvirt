package org.zstack.sso.header

import org.zstack.sso.header.APIUpdateCasClientEvent

doc {
	title "UpdateCasClient"

	category "sso"

	desc """更新 CAS 客户端"""

	rest {
		request {
			url "POST /v1/update/cas/client"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateCasClientMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "CAS 客户端的 UUID，唯一标示该资源"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "CAS 客户端的详细描述"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "CAS 客户端名称"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "casServerLoginUrl"
					enclosedIn "params"
					desc "CAS 服务的登录 URL"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "casServerUrlPrefix"
					enclosedIn "params"
					desc "CAS 服务的 URL 前缀"
					location "body"
					type "String"
					optional true
					since "4.3.0"
				}
				column {
					name "serverName"
					enclosedIn "params"
					desc "MN 的地址，示例：http://127.0.0.1:8080/sso"
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
					since "4.10.6"
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
			clz APIUpdateCasClientEvent.class
		}
	}
}