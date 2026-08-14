package org.zstack.sns.platform.dingtalk

import org.zstack.sns.platform.dingtalk.APICreateSNSDingTalkEndpointEvent

doc {
	title "CreateSNSDingTalkEndpoint"

	category "sns"

	desc """创建钉钉终端"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/ding-talk"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSDingTalkEndpointMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "钉钉机器人URL"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "atAll"
					enclosedIn "params"
					desc "是否消息@所有人"
					location "body"
					type "Boolean"
					optional true
					since "2.3"
				}
				column {
					name "atPersonPhoneNumbers"
					enclosedIn "params"
					desc "要@用户的电话号码"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
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
					enclosedIn "params"
					desc "通知平台UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
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
			clz APICreateSNSDingTalkEndpointEvent.class
		}
	}
}