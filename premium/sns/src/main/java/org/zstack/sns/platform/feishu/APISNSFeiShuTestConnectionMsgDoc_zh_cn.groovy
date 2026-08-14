package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.APISNSFeiShuTestConnectionEvent

doc {
	title "SNSFeiShuTestConnection"

	category "sns"

	desc """测试飞书连通性(发送测试消息)"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/feishu/test-connection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISNSFeiShuTestConnectionMsg.class

			desc """"""

			params {

				column {
					name "testMsg"
					enclosedIn "params"
					desc "测试消息"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "飞书终端UUID"
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
					desc "飞书Webhook URL"
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
					desc "@用户ID"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "secret"
					enclosedIn "params"
					desc "飞书秘钥"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APISNSFeiShuTestConnectionEvent.class
		}
	}
}