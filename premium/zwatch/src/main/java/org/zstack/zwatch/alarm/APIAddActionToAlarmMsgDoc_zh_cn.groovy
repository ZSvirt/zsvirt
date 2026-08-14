package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIAddActionToAlarmEvent

doc {
	title "AddActionToAlarm"

	category "zwatch.alarm"

	desc """添加报警动作"""

	rest {
		request {
			url "POST /v1/zwatch/alarms/{alarmUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddActionToAlarmMsg.class

			desc """"""

			params {

				column {
					name "alarmUuid"
					enclosedIn "params"
					desc "报警器UUID"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "actionUuid"
					enclosedIn "params"
					desc "报警动作UUID"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "actionType"
					enclosedIn "params"
					desc "报警动作类型"
					location "body"
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
			clz APIAddActionToAlarmEvent.class
		}
	}
}