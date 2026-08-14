package org.zstack.monitoring.actions

import org.zstack.monitoring.actions.APIChangeMonitorTriggerActionStateEvent

doc {
	title "ChangeMonitorTriggerActionState"

	category "monitoring"

	desc """更改报警器动作状态"""

	rest {
		request {
			url "PUT /v1/monitoring/trigger-actions/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeMonitorTriggerActionStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeMonitorTriggerActionState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "stateEvent"
					enclosedIn "changeMonitorTriggerActionState"
					desc "状态事件。enable/disable"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
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
			clz APIChangeMonitorTriggerActionStateEvent.class
		}
	}
}