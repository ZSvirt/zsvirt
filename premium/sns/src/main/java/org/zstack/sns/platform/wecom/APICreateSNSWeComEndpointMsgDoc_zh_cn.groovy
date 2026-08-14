package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.APICreateSNSWeComEndpointEvent

doc {
	title "CreateSNSWeComEndpoint"

	category "sns"

	desc """创建SNS企业微信终端"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/we-com"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSWeComEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "企业微信聊天机器人Webhook URL"
					location "body"
					type "String"
					optional false
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
					name "atPersonUserIds"
					enclosedIn "params"
					desc "@用户ID(user_id)"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "platformUuid"
					enclosedIn "params"
					desc "通知平台UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
					name "atPersonList"
					enclosedIn "params"
					desc "@用户信息(推荐使用)"
					location "body"
					type "Map"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APICreateSNSWeComEndpointEvent.class
		}
	}
}