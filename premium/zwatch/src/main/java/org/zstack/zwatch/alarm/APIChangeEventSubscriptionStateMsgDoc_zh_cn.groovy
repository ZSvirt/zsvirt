package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIChangeEventSubscriptionStateEvent

doc {
	title "ChangeEventSubscriptionState"

	category "zwatch.alarm"

	desc """修改事件报警器状态"""

	rest {
		request {
			url "PUT /v1/zwatch/change/eventSubscription/{uuid}/state"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeEventSubscriptionStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeEventSubscriptionState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "state"
					enclosedIn "changeEventSubscriptionState"
					desc "事件报警器状态"
					location "body"
					type "String"
					optional false
					since "3.17.0"
					values ("Enabled","Disabled")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APIChangeEventSubscriptionStateEvent.class
		}
	}
}