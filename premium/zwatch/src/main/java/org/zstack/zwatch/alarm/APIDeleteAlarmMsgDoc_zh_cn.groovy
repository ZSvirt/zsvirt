package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIDeleteAlarmEvent

doc {
	title "DeleteAlarm"

	category "zwatch.alarm"

	desc """删除报警器"""

	rest {
		request {
			url "DELETE /v1/zwatch/alarms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAlarmMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteAlarmEvent.class
		}
	}
}