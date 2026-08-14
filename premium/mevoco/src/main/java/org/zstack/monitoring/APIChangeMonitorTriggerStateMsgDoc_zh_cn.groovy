package org.zstack.monitoring

import org.zstack.monitoring.APIChangeMonitorTriggerStateEvent

doc {
	title "ChangeMonitorTriggerState"

	category "monitoring"

	desc """修改报警器状态"""

	rest {
		request {
			url "PUT /v1/monitoring/triggers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeMonitorTriggerStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeMonitorTriggerState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "stateEvent"
					enclosedIn "changeMonitorTriggerState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "2.1"
					values ("enable","disable")
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
			clz APIChangeMonitorTriggerStateEvent.class
		}
	}
}