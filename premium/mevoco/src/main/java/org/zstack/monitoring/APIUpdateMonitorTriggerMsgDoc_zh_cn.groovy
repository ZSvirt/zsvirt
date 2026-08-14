package org.zstack.monitoring

import org.zstack.monitoring.APIUpdateMonitorTriggerEvent

doc {
	title "UpdateMonitorTrigger"

	category "monitoring"

	desc """更新报警器"""

	rest {
		request {
			url "PUT /v1/monitoring/triggers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateMonitorTriggerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateMonitorTrigger"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "updateMonitorTrigger"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateMonitorTrigger"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "expression"
					enclosedIn "updateMonitorTrigger"
					desc "报警表达式"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "duration"
					enclosedIn "updateMonitorTrigger"
					desc "持续时间"
					location "body"
					type "Integer"
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
			clz APIUpdateMonitorTriggerEvent.class
		}
	}
}