package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIRemoveLabelFromAlarmEvent

doc {
	title "RemoveLabelFromAlarm"

	category "zwatch.alarm"

	desc """删除报警器标签"""

	rest {
		request {
			url "DELETE /v1/zwatch/alarms/labels/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveLabelFromAlarmMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
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
			clz APIRemoveLabelFromAlarmEvent.class
		}
	}
}