package org.zstack.sns

import org.zstack.sns.APICreateSNSTopicEvent

doc {
	title "CreateSNSTopic"

	category "sns"

	desc """创建SNS主题"""

	rest {
		request {
			url "POST /v1/sns/topics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSTopicMsg.class

			desc """"""

			params {

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
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
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
					name "locale"
					enclosedIn "params"
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
			clz APICreateSNSTopicEvent.class
		}
	}
}