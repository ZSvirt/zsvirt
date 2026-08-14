package org.zstack.sns.platform.http

import org.zstack.sns.platform.http.APICreateSNSHttpEndpointEvent

doc {
	title "CreateSNSHttpEndpoint"

	category "sns"

	desc """创建SNS HTTP终端"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/http"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSHttpEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "HTTP URL"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "username"
					enclosedIn "params"
					desc "用户名"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "platformUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateSNSHttpEndpointEvent.class
		}
	}
}