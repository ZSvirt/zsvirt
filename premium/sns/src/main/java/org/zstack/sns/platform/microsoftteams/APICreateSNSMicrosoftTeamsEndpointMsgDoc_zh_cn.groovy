package org.zstack.sns.platform.microsoftteams

import org.zstack.sns.platform.microsoftteams.APICreateSNSMicrosoftTeamsEndpointEvent

doc {
	title "CreateSNSMicrosoftTeamsEndpoint"

	category "sns"

	desc """创建微软Teams接收端"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/microsoft-teams"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSMicrosoftTeamsEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "连接器的url"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "platformUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APICreateSNSMicrosoftTeamsEndpointEvent.class
		}
	}
}