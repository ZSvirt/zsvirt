package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIAddLabelToAlarmEvent

doc {
	title "AddLabelToAlarm"

	category "zwatch.alarm"

	desc """添加标签到报警器"""

	rest {
		request {
			url "POST /v1/zwatch/alarms/{alarmUuid}/labels"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddLabelToAlarmMsg.class

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
					name "key"
					enclosedIn "params"
					desc "标签键"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "value"
					enclosedIn "params"
					desc "标签值"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "operator"
					enclosedIn "params"
					desc "操作符"
					location "body"
					type "String"
					optional false
					since "2.3"
					values ("Regex","Equal")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
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
			clz APIAddLabelToAlarmEvent.class
		}
	}
}