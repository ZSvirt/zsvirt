package org.zstack.sns.platform.wecom

import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent

doc {
	title "UpdateSNSWeComEndpoint"

	category "sns"

	desc """更新企业微信终端"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/we-com/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSWeComEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "updateSNSWeComEndpoint"
					desc "企业微信Webhook URL"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "atAll"
					enclosedIn "updateSNSWeComEndpoint"
					desc "@所有人"
					location "body"
					type "Boolean"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "uuid"
					enclosedIn "updateSNSWeComEndpoint"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "name"
					enclosedIn "updateSNSWeComEndpoint"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "description"
					enclosedIn "updateSNSWeComEndpoint"
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
					enclosedIn "updateSNSWeComEndpoint"
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