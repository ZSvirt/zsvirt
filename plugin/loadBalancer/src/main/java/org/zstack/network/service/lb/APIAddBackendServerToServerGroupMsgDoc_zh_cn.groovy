package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddBackendServerToServerGroupEvent

doc {
	title "添加后端服务器到服务器组"

	category "负载均衡"

	desc """负载均衡器服务器组添加后端服务器"""

	rest {
		request {
			url "POST /v1/load-balancers/servergroups/{serverGroupUuid}/backendservers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddBackendServerToServerGroupMsg.class

			desc """"""

			params {

				column {
					name "serverGroupUuid"
					enclosedIn "params"
					desc "负载均衡器服务器组uuid"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "vmNics"
					enclosedIn "params"
					desc "后端服务器网卡"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "servers"
					enclosedIn "params"
					desc "后端服务器Ip"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIAddBackendServerToServerGroupEvent.class
		}
	}
}