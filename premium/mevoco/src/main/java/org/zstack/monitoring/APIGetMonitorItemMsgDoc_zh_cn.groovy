package org.zstack.monitoring

import org.zstack.monitoring.APIGetMonitorItemReply

doc {
	title "GetMonitorItem"

	category "monitoring"

	desc """获取报警条目"""

	rest {
		request {
			url "GET /v1/monitoring/items"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetMonitorItemMsg.class

			desc """"""

			params {

				column {
					name "resourceType"
					enclosedIn ""
					desc "资源类型"
					location "query"
					type "String"
					optional false
					since "2.1"
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
			clz APIGetMonitorItemReply.class
		}
	}
}