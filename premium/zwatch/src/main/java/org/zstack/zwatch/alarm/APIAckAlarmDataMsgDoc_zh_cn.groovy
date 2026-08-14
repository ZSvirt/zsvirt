package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIAckAlertDataEvent

doc {
	title "AckAlarmData"

	category "zwatch"

	desc """确认资源报警消息"""

	rest {
		request {
			url "POST /v1/zwatch/alarm-histories/acknowledgments"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAckAlarmDataMsg.class

			desc """"""

			params {

				column {
					name "alarmUuid"
					enclosedIn "params"
					desc "报警器UUID"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "alertDataUuid"
					enclosedIn "params"
					desc "报警消息UUID"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "dataType"
					enclosedIn "params"
					desc "消息类型"
					location "body"
					type "String"
					optional false
					since "3.10.0"
					values ("alarm","event")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "报警目标资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "ackPeriodSec"
					enclosedIn "params"
					desc "沉默时间"
					location "body"
					type "Integer"
					optional false
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIAckAlertDataEvent.class
		}
	}
}