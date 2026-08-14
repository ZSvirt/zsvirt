package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIUpdateEventSubscriptionLabelEvent

doc {
	title "UpdateEventSubscriptionLabel"

	category "zwatch.alarm"

	desc """更新事件订阅的标签"""

	rest {
		request {
			url "PUT /v1/zwatch/events/subscriptions/labels/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateEventSubscriptionLabelMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateEventSubscriptionLabel"
					desc "原事件订阅标签的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "key"
					enclosedIn "updateEventSubscriptionLabel"
					desc "标签的新名"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "value"
					enclosedIn "updateEventSubscriptionLabel"
					desc "标签的新值"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "operator"
					enclosedIn "updateEventSubscriptionLabel"
					desc "标签的新操作符"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("Regex","Equal")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateEventSubscriptionLabelEvent.class
		}
	}
}