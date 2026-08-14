package org.zstack.header.cloudformation.monitor

import org.zstack.header.cloudformation.monitor.APIDeleteResourceStackVmPortMonitorEvent

doc {
	title "DeleteResourceStackVmPortMonitor"

	category "cloudformation"

	desc """删除资源栈中云主机端口监控"""

	rest {
		request {
			url "DELETE /v1/cloudformation/stack/monitor/delvm"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteResourceStackVmPortMonitorMsg.class

			desc """"""

			params {

				column {
					name "stackUuid"
					enclosedIn ""
					desc "资源栈UUID"
					location "query"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "query"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "port"
					enclosedIn ""
					desc "端口号"
					location "query"
					type "Integer"
					optional true
					since "3.9.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
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
			clz APIDeleteResourceStackVmPortMonitorEvent.class
		}
	}
}