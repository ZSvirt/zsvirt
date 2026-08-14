package org.zstack.sns

import org.zstack.sns.APIChangeSNSTopicStateEvent

doc {
	title "ChangeSNSTopicState"

	category "sns"

	desc """更改SNS主题状态"""

	rest {
		request {
			url "PUT /v1/zwatch/topics/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeSNSTopicStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeSNSTopicState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "stateEvent"
					enclosedIn "changeSNSTopicState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "2.3"
					values ("enable","disable")
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
			}
		}

		response {
			clz APIChangeSNSTopicStateEvent.class
		}
	}
}