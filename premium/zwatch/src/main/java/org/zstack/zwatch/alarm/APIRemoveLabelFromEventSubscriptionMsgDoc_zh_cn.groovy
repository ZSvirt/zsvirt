package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIRemoveLabelFromEventSubscriptionEvent

doc {
	title "RemoveLabelFromEventSubscription"

	category "zwatch.alarm"

	desc """从事件订阅中删除标签"""

	rest {
		request {
			url "DELETE /v1/zwatch/events/subscriptions/labels/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveLabelFromEventSubscriptionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "标签UUID"
					location "url"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.3.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.3.1"
				}
			}
		}

		response {
			clz APIRemoveLabelFromEventSubscriptionEvent.class
		}
	}
}