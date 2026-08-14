package org.zstack.drs.api

import org.zstack.drs.api.APIExecuteDRSSchedulingEvent

doc {
	title "ExecuteDRSScheduling"

	category "drs"

	desc """执行DRS调度"""

	rest {
		request {
			url "PUT /v1/clusters/drs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExecuteDRSSchedulingMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "executeDRSScheduling"
					desc "集群DRS的UUID，不是集群UUID"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
			}
		}

		response {
			clz APIExecuteDRSSchedulingEvent.class
		}
	}
}