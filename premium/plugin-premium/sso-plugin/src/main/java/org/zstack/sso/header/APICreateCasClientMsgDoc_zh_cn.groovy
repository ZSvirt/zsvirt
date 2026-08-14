package org.zstack.sso.header

import org.zstack.sso.header.APICreateCasClientEvent

doc {
	title "CreateCasClient"

	category "sso"

	desc """创建 CAS 客户端"""

	rest {
		request {
			url "POST /v1/create/cas/client"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateCasClientMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "4.10.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.10.6"
				}
				column {
					name "casServerLoginUrl"
					enclosedIn "params"
					desc "CAS 服务的登录 URL"
					location "body"
					type "String"
					optional false
					since "4.10.6"
				}
				column {
					name "casServerUrlPrefix"
					enclosedIn "params"
					desc "CAS 服务的 URL 前缀"
					location "body"
					type "String"
					optional false
					since "4.10.6"
				}
				column {
					name "serverName"
					enclosedIn "params"
					desc "MN 的地址，示例：http://127.0.0.1:8080/sso"
					location "body"
					type "String"
					optional false
					since "4.10.6"
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
					name "urlTemplate"
					enclosedIn "params"
					desc "免密登录之后的跳转的模板"
					location "body"
					type "String"
					optional true
					since "4.10.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源 UUID"
					location "body"
					type "String"
					optional true
					since "4.10.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签 UUID 列表"
					location "body"
					type "List"
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
					since "4.10.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.6"
				}
			}
		}

		response {
			clz APICreateCasClientEvent.class
		}
	}
}