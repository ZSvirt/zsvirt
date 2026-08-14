package org.zstack.monitoring

import org.zstack.monitoring.APIDeleteAlertEvent

doc {
	title "DeleteAlert"

	category "monitoring"

	desc """删除报警记录"""

	rest {
		request {
			url "DELETE /v1/monitoring/alerts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAlertMsg.class

			desc """"""

			params {

				column {
					name "uuids"
					enclosedIn ""
					desc "报警记录UUID"
					location "query"
					type "List"
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
			clz APIDeleteAlertEvent.class
		}
	}
}