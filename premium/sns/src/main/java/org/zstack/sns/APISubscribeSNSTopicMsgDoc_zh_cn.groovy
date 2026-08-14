package org.zstack.sns

import org.zstack.sns.APISubscribeSNSTopicEvent

doc {
	title "SubscribeSNSTopic"

	category "sns"

	desc """订阅SNS应用主题"""

	rest {
		request {
			url "POST /v1/sns/topics/{topicUuid}/endpoints/{endpointUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISubscribeSNSTopicMsg.class

			desc """"""

			params {

				column {
					name "topicUuid"
					enclosedIn "params"
					desc "应用主题UUID"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
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
			}
		}

		response {
			clz APISubscribeSNSTopicEvent.class
		}
	}
}