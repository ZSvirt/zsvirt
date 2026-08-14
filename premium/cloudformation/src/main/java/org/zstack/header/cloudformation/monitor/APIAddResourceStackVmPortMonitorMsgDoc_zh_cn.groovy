package org.zstack.header.cloudformation.monitor

import org.zstack.header.cloudformation.monitor.APIAddResourceStackVmPortMonitorEvent

doc {
	title "AddResourceStackVmPortMonitor"

	category "cloudformation"

	desc """添加资源栈中云主机的端口监控"""

	rest {
		request {
			url "POST /v1/cloudformation/stack/monitor/addvm"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddResourceStackVmPortMonitorMsg.class

			desc """"""

			params {

				column {
					name "stackUuid"
					enclosedIn "params"
					desc "资源栈UUID"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "port"
					enclosedIn "params"
					desc "端口号"
					location "body"
					type "Integer"
					optional false
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIAddResourceStackVmPortMonitorEvent.class
		}
	}
}