package org.zstack.monitoring

import org.zstack.monitoring.APIDeleteMonitorTriggerEvent

doc {
	title "DeleteMonitorTrigger"

	category "monitoring"

	desc """删除报警器"""

	rest {
		request {
			url "DELETE /v1/monitoring/triggers/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteMonitorTriggerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
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
			clz APIDeleteMonitorTriggerEvent.class
		}
	}
}