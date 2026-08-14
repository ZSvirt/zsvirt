package org.zstack.sns.platform.dingtalk

import org.zstack.sns.APIUpdateSNSApplicationEndpointEvent

doc {
	title "UpdateSNSDingTalkEndpoint"

	category "sns"

	desc """更新钉钉终端"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/ding-talk/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSDingTalkEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "updateSNSDingTalkEndpoint"
					desc "钉钉Webhook URL"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "atAll"
					enclosedIn "updateSNSDingTalkEndpoint"
					desc "@所有人"
					location "body"
					type "Boolean"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "secret"
					enclosedIn "updateSNSDingTalkEndpoint"
					desc "钉钉秘钥"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "uuid"
					enclosedIn "updateSNSDingTalkEndpoint"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "name"
					enclosedIn "updateSNSDingTalkEndpoint"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "description"
					enclosedIn "updateSNSDingTalkEndpoint"
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
					enclosedIn "updateSNSDingTalkEndpoint"
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