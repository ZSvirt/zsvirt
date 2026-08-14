package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIAckAlertDataEvent

doc {
	title "AckAlertData"

	category "确认报警消息"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/zwatch/alert-histories/acknowledgments"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAckAlertDataMsg.class

			desc """"""

			params {

				column {
					name "alertDataUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "dataType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "3.10.0"
					values ("alarm","event")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "ackPeriodSec"
					enclosedIn "params"
					desc ""
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