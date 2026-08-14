package org.zstack.sns

import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent

doc {
	title "UpdateSNSApplicationEndpoint"

	category "sns"

	desc """更新SNS应用终端"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSApplicationEndpointMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSNSApplicationEndpoint"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "updateSNSApplicationEndpoint"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "updateSNSApplicationEndpoint"
					desc "资源的详细描述"
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
					enclosedIn "updateSNSApplicationEndpoint"
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