package org.zstack.zwatch.thirdparty.api

import org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyAlertsEvent

doc {
	title "UpdateThirdpartyAlerts"

	category "zwatch"

	desc """修改第三方消息"""

	rest {
		request {
			url "PUT /v1/zwatch/third-party/alerts/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateThirdpartyAlertsMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateThirdpartyAlerts"
					desc "消息UUID"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "startTimeMillis"
					enclosedIn "updateThirdpartyAlerts"
					desc "修改在此之后产生的消息。内容为时间戳，毫秒"
					location "body"
					type "Long"
					optional true
					since "3.10"
				}
				column {
					name "endTimeMillis"
					enclosedIn "updateThirdpartyAlerts"
					desc "修改在此之前产生的消息。内容为时间戳，毫秒"
					location "body"
					type "Long"
					optional true
					since "3.10"
				}
				column {
					name "updateReadStatus"
					enclosedIn "updateThirdpartyAlerts"
					desc "修改消息已读状态"
					location "body"
					type "String"
					optional true
					since "3.10"
					values ("Unread","Read")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
			}
		}

		response {
			clz APIUpdateThirdpartyAlertsEvent.class
		}
	}
}