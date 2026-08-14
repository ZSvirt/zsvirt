package org.zstack.zwatch.alarm.activealarm.api

import org.zstack.zwatch.alarm.activealarm.api.APIChangeActiveAlarmStateEvent

doc {
	title "ChangeActiveAlarmState"

	category "zwatch"

	desc """修改一键报警状态"""

	rest {
		request {
			url "POST /v1/zwatch/activealarms/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeActiveAlarmStateMsg.class

			desc """"""

			params {

				column {
					name "namespace"
					enclosedIn "params"
					desc "名字空间"
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "stateEvent"
					enclosedIn "params"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "3.10.0"
					values ("enable","disable")
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
			clz APIChangeActiveAlarmStateEvent.class
		}
	}
}