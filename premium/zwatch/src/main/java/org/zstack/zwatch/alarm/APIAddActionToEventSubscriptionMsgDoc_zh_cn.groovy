package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIAddActionToEventSubscriptionEvent

doc {
	title "AddActionToEventSubscription"

	category "zwatch.alarm"

	desc """添加动作到事件订阅"""

	rest {
		request {
			url "POST /v1/zwatch/events/subscriptions/{subscriptionUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddActionToEventSubscriptionMsg.class

			desc """"""

			params {

				column {
					name "subscriptionUuid"
					enclosedIn "params"
					desc "事件订阅UUID"
					location "url"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "actionUuid"
					enclosedIn "params"
					desc "动作UUID"
					location "body"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "actionType"
					enclosedIn "params"
					desc "动作类型"
					location "body"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.3.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.3.1"
				}
			}
		}

		response {
			clz APIAddActionToEventSubscriptionEvent.class
		}
	}
}