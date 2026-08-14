package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIRemoveActionFromAlarmEvent

doc {
	title "RemoveActionFromAlarm"

	category "zwatch.alarm"

	desc """删除报警动作"""

	rest {
		request {
			url "DELETE /v1/zwatch/alarms/{alarmUuid}/actions/{actionUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveActionFromAlarmMsg.class

			desc """"""

			params {

				column {
					name "alarmUuid"
					enclosedIn ""
					desc "报警器UUID"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "actionUuid"
					enclosedIn ""
					desc "报警动作UUID"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
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
			clz APIRemoveActionFromAlarmEvent.class
		}
	}
}