package org.zstack.monitoring.actions

import org.zstack.monitoring.actions.APIUpdateMonitorTriggerActionEvent

doc {
	title "UpdateEmailMonitorTriggerAction"

	category "monitoring"

	desc """修改Email报警动作"""

	rest {
		request {
			url "PUT /v1/monitoring/trigger-actions/emails/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateEmailMonitorTriggerActionMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateEmailMonitorTriggerAction"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateEmailMonitorTriggerAction"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "email"
					enclosedIn "updateEmailMonitorTriggerAction"
					desc "email地址"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "mediaUuid"
					enclosedIn "updateEmailMonitorTriggerAction"
					desc "包含SMTP信息的Email媒体UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateEmailMonitorTriggerAction"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateMonitorTriggerActionEvent.class
		}
	}
}