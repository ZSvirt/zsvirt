package org.zstack.sns.platform.microsoftteams

import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent

doc {
	title "SNSMicrosoftTeamsTestConnection"

	category "sns"

	desc """测试Microsoft Teams连通性"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/microsoft-teams/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSMicrosoftTeamsEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "updateSNSMicrosoftTeamsEndpoint"
					desc "Microsoft Teams Webhook URL"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "uuid"
					enclosedIn "updateSNSMicrosoftTeamsEndpoint"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "name"
					enclosedIn "updateSNSMicrosoftTeamsEndpoint"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "description"
					enclosedIn "updateSNSMicrosoftTeamsEndpoint"
					desc "资源的详细描述"
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
					name "platformUuid"
					enclosedIn "updateSNSMicrosoftTeamsEndpoint"
					desc "平台uuid"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIUpdateSNSApplicationEndpointEvent.class
		}
	}
}