package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIUpdateAlarmEvent

doc {
	title "UpdateAlarm"

	category "zwatch.alarm"

	desc """更新报警器"""

	rest {
		request {
			url "PUT /v1/zwatch/alarms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAlarmMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAlarm"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "updateAlarm"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "updateAlarm"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "comparisonOperator"
					enclosedIn "updateAlarm"
					desc "阈值比较符"
					location "body"
					type "String"
					optional true
					since "2.3"
					values ("GreaterThanOrEqualTo","GreaterThan","LessThan","LessThanOrEqualTo")
				}
				column {
					name "period"
					enclosedIn "updateAlarm"
					desc "阈值持续时间"
					location "body"
					type "Integer"
					optional true
					since "2.3"
				}
				column {
					name "threshold"
					enclosedIn "updateAlarm"
					desc "阈值"
					location "body"
					type "Double"
					optional true
					since "2.3"
				}
				column {
					name "repeatInterval"
					enclosedIn "updateAlarm"
					desc "报警重复时间"
					location "body"
					type "Integer"
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
					name "repeatCount"
					enclosedIn "updateAlarm"
					desc "报警重复次数"
					location "body"
					type "Integer"
					optional true
					since "3.2"
				}
				column {
					name "emergencyLevel"
					enclosedIn "updateAlarm"
					desc "报警等级"
					location "body"
					type "String"
					optional true
					since "3.8"
					values ("Emergent","Important","Normal")
				}
				column {
					name "enableRecovery"
					enclosedIn "updateAlarm"
					desc ""
					location "body"
					type "Boolean"
					optional true
					since "0.6"
				}
				column {
					name "actions"
					enclosedIn "updateAlarm"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateAlarmEvent.class
		}
	}
}