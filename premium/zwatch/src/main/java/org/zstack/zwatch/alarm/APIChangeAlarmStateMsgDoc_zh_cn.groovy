package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIChangeAlarmStateEvent

doc {
	title "ChangeAlarmState"

	category "zwatch.alarm"

	desc """改变报警器状态"""

	rest {
		request {
			url "PUT /v1/zwatch/alarms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAlarmStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeAlarmState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "stateEvent"
					enclosedIn "changeAlarmState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "2.3"
					values ("enabled","disabled")
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
			clz APIChangeAlarmStateEvent.class
		}
	}
}