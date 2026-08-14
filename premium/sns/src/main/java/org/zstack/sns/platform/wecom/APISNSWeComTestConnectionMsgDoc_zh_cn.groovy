package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.APISNSWeComTestConnectionEvent

doc {
	title "SNSWeComTestConnection"

	category "sns"

	desc """SNS企业微信测试连通性(发送测试消息)"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/we-com/test-connection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISNSWeComTestConnectionMsg.class

			desc """"""

			params {

				column {
					name "testMsg"
					enclosedIn "params"
					desc "测试文本"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "企业微信终端UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "企业微信Webhook URL"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "atAll"
					enclosedIn "params"
					desc "@所有人"
					location "body"
					type "Boolean"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "atPersonUserIds"
					enclosedIn "params"
					desc "@用户ID集合"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APISNSWeComTestConnectionEvent.class
		}
	}
}