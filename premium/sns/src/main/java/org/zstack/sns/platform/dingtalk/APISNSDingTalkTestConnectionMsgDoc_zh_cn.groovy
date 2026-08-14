package org.zstack.sns.platform.dingtalk

import org.zstack.sns.platform.dingtalk.APISNSDingTalkTestConnectionEvent

doc {
	title "SNSDingTalkTestConnection"

	category "sns"

	desc """测试钉钉连通性(发送测试消息)"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/ding-talk/test-connection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISNSDingTalkTestConnectionMsg.class

			desc """"""

			params {

				column {
					name "testMsg"
					enclosedIn "params"
					desc "测试消息文本"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "钉钉终端UUID"
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
					desc "Webhook URL"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "atAll"
					enclosedIn "params"
					desc "是否@所有人"
					location "body"
					type "Boolean"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "atPersonPhoneNumbers"
					enclosedIn "params"
					desc "@手机号"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "secret"
					enclosedIn "params"
					desc "钉钉秘钥"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APISNSDingTalkTestConnectionEvent.class
		}
	}
}