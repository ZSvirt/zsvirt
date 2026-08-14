package org.zstack.sns.platform.http

import org.zstack.sns.platform.http.APISNSHttpTestConnectionEvent

doc {
	title "SNSHttpTestConnection"

	category "sns"

	desc """发送测试HTTP请求"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/http/test-connection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISNSHttpTestConnectionMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "webhook url"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "username"
					enclosedIn "params"
					desc "Basic Auth 用户名"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "password"
					enclosedIn "params"
					desc "Basic Auth 密码"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "端点uuid"
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
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APISNSHttpTestConnectionEvent.class
		}
	}
}