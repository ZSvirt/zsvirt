package org.zstack.sns

import org.zstack.sns.APIUnsubscribeSNSTopicEvent

doc {
	title "UnsubscribeSNSTopic"

	category "sns"

	desc """退订SNS主题"""

	rest {
		request {
			url "DELETE /v1/sns/topics/{topicUuid}/endpoints/{endpointUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUnsubscribeSNSTopicMsg.class

			desc """"""

			params {

				column {
					name "topicUuid"
					enclosedIn ""
					desc "主题UUID"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "endpointUuid"
					enclosedIn ""
					desc "终端UUID"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIUnsubscribeSNSTopicEvent.class
		}
	}
}