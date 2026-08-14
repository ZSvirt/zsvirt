package org.zstack.zwatch.alarm.activealarm.api

import org.zstack.zwatch.alarm.activealarm.api.APIGetActiveAlarmStatusReply

doc {
	title "GetActiveAlarmStatus"

	category "zwatch"

	desc """查询一键报警状态"""

	rest {
		request {
			url "GET /v1/zwatch/activealarms/status"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetActiveAlarmStatusMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn ""
					desc "账户UUID"
					location "query"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIGetActiveAlarmStatusReply.class
		}
	}
}