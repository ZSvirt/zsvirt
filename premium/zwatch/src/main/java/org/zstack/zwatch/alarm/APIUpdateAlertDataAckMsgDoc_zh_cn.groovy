package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIUpdateAlertDataAckEvent

doc {
	title "UpdateAlertDataAck"

	category "zwatch"

	desc """更新报警确认信息"""

	rest {
		request {
			url "PUT /v1/zwatch/alert-histories/acknowledgments/{alertDataUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAlertDataAckMsg.class

			desc """"""

			params {

				column {
					name "alertDataUuid"
					enclosedIn "updateAlertDataAck"
					desc "报警消息UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "resumeAlert"
					enclosedIn "updateAlertDataAck"
					desc "恢复报警"
					location "body"
					type "Boolean"
					optional true
					since "0.6"
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
			clz APIUpdateAlertDataAckEvent.class
		}
	}
}