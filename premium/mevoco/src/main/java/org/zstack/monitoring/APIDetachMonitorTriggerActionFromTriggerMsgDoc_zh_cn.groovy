package org.zstack.monitoring

import org.zstack.monitoring.APIDetachMonitorTriggerActionFromTriggerEvent

doc {
	title "DetachMonitorTriggerActionFromTrigger"

	category "monitoring"

	desc """卸载报警动作"""

	rest {
		request {
			url "DELETE /v1/monitoring/triggers/{triggerUuid}/trigger-actions/{actionUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachMonitorTriggerActionFromTriggerMsg.class

			desc """"""

			params {

				column {
					name "triggerUuid"
					enclosedIn ""
					desc "报警器UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "actionUuid"
					enclosedIn ""
					desc "报警动作UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDetachMonitorTriggerActionFromTriggerEvent.class
		}
	}
}