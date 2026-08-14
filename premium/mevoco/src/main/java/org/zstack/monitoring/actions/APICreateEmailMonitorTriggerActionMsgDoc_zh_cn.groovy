package org.zstack.monitoring.actions

import org.zstack.monitoring.actions.APICreateMonitorTriggerActionEvent

doc {
	title "CreateEmailMonitorTriggerAction"

	category "monitoring"

	desc """创建Email报警动作"""

	rest {
		request {
			url "POST /v1/monitoring/trigger-actions/emails"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateEmailMonitorTriggerActionMsg.class

			desc """"""

			params {

				column {
					name "email"
					enclosedIn "params"
					desc "email地址"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "mediaUuid"
					enclosedIn "params"
					desc "包含SMTP信息的Email媒体UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "triggerUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreateMonitorTriggerActionEvent.class
		}
	}
}