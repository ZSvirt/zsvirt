package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRefreshLoadBalancerEvent

doc {
	title "刷新负载均衡器(RefreshLoadBalancer)"

	category "负载均衡"

	desc """刷新负载均衡器"""

	rest {
		request {
			url "PUT /v1/load-balancers/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRefreshLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "refreshLoadBalancer"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIRefreshLoadBalancerEvent.class
		}
	}
}