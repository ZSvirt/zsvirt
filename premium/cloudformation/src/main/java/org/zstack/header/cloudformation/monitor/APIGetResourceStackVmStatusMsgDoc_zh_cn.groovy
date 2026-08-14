package org.zstack.header.cloudformation.monitor

import org.zstack.header.cloudformation.monitor.APIGetResourceStackVmStatusReply

doc {
	title "GetResourceStackVmStatus"

	category "cloudformation"

	desc """获取资源栈中云主机端口监控状态"""

	rest {
		request {
			url "GET /v1/cloudformation/stack/monitor/vmstatus"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceStackVmStatusMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIGetResourceStackVmStatusReply.class
		}
	}
}