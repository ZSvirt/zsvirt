package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIUpdateAlarmLabelEvent

doc {
	title "UpdateAlarmLabel"

	category "zwatch.alarm"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/zwatch/alarms/labels/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAlarmLabelMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAlarmLabel"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "key"
					enclosedIn "updateAlarmLabel"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "value"
					enclosedIn "updateAlarmLabel"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "operator"
					enclosedIn "updateAlarmLabel"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("Regex","Equal")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateAlarmLabelEvent.class
		}
	}
}