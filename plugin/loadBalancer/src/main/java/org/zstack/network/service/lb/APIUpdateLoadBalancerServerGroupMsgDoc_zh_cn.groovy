package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIUpdateLoadBalancerServerGroupEvent

doc {
	title "更新负载均衡服务器组"

	category "负载均衡"

	desc """更新负载均衡服务器组"""

	rest {
		request {
			url "PUT /v1/load-balancers/servergroups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateLoadBalancerServerGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateLoadBalancerServerGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "name"
					enclosedIn "updateLoadBalancerServerGroup"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "description"
					enclosedIn "updateLoadBalancerServerGroup"
					desc "资源的详细描述"
					location "body"
					type "String"
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
			clz APIUpdateLoadBalancerServerGroupEvent.class
		}
	}
}