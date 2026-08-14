package org.zstack.sns.platform.microsoftteams

import org.zstack.sns.platform.microsoftteams.APISNSMicrosoftTeamsTestConnectionEvent

doc {
	title "SNSMicrosoftTeamsTestConnection"

	category "sns"

	desc """Microsoft Teams测试连通性(发送测试消息)"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/microsoft-teams/test-connection"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISNSMicrosoftTeamsTestConnectionMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "Microsoft Teams Webhook URL"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
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
					desc "Microsoft Teams终端UUID"
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
			}
		}

		response {
			clz APISNSMicrosoftTeamsTestConnectionEvent.class
		}
	}
}