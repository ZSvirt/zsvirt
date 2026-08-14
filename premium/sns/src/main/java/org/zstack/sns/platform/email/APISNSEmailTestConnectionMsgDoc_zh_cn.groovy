package org.zstack.sns.platform.email

import org.zstack.sns.platform.email.APISNSEmailTestConnectionEvent

doc {
	title "SNSEmailTestConnection"

	category "sns"

	desc """发送测试邮件"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/email/test-connection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISNSEmailTestConnectionMsg.class

			desc """"""

			params {

				column {
					name "emails"
					enclosedIn "params"
					desc "接收邮箱地址接"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "platformUuid"
					enclosedIn "params"
					desc "邮箱平台uuid"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "邮箱端点uuid"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "subject"
					enclosedIn "params"
					desc "主题"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "text"
					enclosedIn "params"
					desc "内容"
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
			clz APISNSEmailTestConnectionEvent.class
		}
	}
}