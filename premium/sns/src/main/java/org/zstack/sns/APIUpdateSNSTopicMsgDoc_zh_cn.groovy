package org.zstack.sns

import org.zstack.sns.APIUpdateSNSTopicEvent

doc {
	title "UpdateSNSTopic"

	category "sns"

	desc """更新SNS主题"""

	rest {
		request {
			url "PUT /v1/sns/topics/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSTopicMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSNSTopic"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "updateSNSTopic"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "updateSNSTopic"
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
					name "locale"
					enclosedIn "updateSNSTopic"
					desc "国际化语言"
					location "body"
					type "String"
					optional true
					since "3.16.21"
					values ("zh_CN","en_US")
				}
			}
		}

		response {
			clz APIUpdateSNSTopicEvent.class
		}
	}
}