package org.zstack.sns.platform.http

import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent

doc {
	title "UpdateSNSHttpEndpoint"

	category "sns"

	desc """更新HTTP终端"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/http/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSHttpEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "updateSNSHttpEndpoint"
					desc "HTTP Webhook URL"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "username"
					enclosedIn "updateSNSHttpEndpoint"
					desc "用户名"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "password"
					enclosedIn "updateSNSHttpEndpoint"
					desc "密码"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "uuid"
					enclosedIn "updateSNSHttpEndpoint"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "name"
					enclosedIn "updateSNSHttpEndpoint"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "description"
					enclosedIn "updateSNSHttpEndpoint"
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
					enclosedIn "updateSNSHttpEndpoint"
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