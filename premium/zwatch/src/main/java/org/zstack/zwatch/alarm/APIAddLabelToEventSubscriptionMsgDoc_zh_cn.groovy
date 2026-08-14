package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIAddLabelToEventSubscriptionEvent

doc {
	title "AddLabelToEventSubscription"

	category "zwatch.alarm"

	desc """添加标签到事件订阅"""

	rest {
		request {
			url "POST /v1/zwatch/events/subscriptions/{subscriptionUuid}/labels"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddLabelToEventSubscriptionMsg.class

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
					name "key"
					enclosedIn "params"
					desc "标签名"
					location "body"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "value"
					enclosedIn "params"
					desc "标签值"
					location "body"
					type "String"
					optional false
					since "2.3.1"
				}
				column {
					name "operator"
					enclosedIn "params"
					desc "标签操作符"
					location "body"
					type "String"
					optional false
					since "2.3.1"
					values ("Regex","Equal")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddLabelToEventSubscriptionEvent.class
		}
	}
}