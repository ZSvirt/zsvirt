package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIRemoveActionFromEventSubscriptionEvent

doc {
	title "RemoveActionFromEventSubscription"

	category "zwatch.alarm"

	desc """从事件订阅里删除动作"""

	rest {
		request {
			url "DELETE /v1/zwatch/events/subscriptions/{subscriptionUuid}/actions/{actionUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveActionFromEventSubscriptionMsg.class

			desc """"""

			params {

				column {
					name "subscriptionUuid"
					enclosedIn ""
					desc "事件订阅UUID"
					location "url"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "actionUuid"
					enclosedIn ""
					desc "动作UUID"
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
			clz APIRemoveActionFromEventSubscriptionEvent.class
		}
	}
}