package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIChangeLoadBalancerBackendServerEvent

doc {
	title "修改负载均衡后端服务器参数"

	category "负载均衡"

	desc """修改负载均衡后端服务器参数"""

	rest {
		request {
			url "PUT /v1/load-balancers/servergroups/{serverGroupUuid}/backendserver/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeLoadBalancerBackendServerMsg.class

			desc """"""

			params {

				column {
					name "serverGroupUuid"
					enclosedIn "changeLoadBalancerBackendServer"
					desc "负载均衡器服务器组uuid"
					location "url"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "vmNics"
					enclosedIn "changeLoadBalancerBackendServer"
					desc "后端服务器组网卡"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "servers"
					enclosedIn "changeLoadBalancerBackendServer"
					desc "后端服务器组网ip"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.10"
				}
			}
		}

		response {
			clz APIChangeLoadBalancerBackendServerEvent.class
		}
	}
}