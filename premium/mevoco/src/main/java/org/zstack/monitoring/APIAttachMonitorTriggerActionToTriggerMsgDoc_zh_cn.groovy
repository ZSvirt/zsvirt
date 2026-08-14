package org.zstack.monitoring

import org.zstack.monitoring.APIAttachMonitorTriggerActionToTriggerEvent

doc {
	title "AttachMonitorTriggerActionToTrigger"

	category "monitoring"

	desc """加载报警动作到报警器"""

	rest {
		request {
			url "POST /v1/monitoring/triggers/{triggerUuid}/trigger-actions/{actionUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachMonitorTriggerActionToTriggerMsg.class

			desc """"""

			params {

				column {
					name "triggerUuid"
					enclosedIn "params"
					desc "报警器UUID"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "actionUuid"
					enclosedIn "params"
					desc "报警动作UUID"
					location "url"
					type "String"
					optional false
					since "2.1"
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
			clz APIAttachMonitorTriggerActionToTriggerEvent.class
		}
	}
}