package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddServerGroupToLoadBalancerListenerEvent

doc {
	title "AddServerGroupToLoadBalancerListener"

	category "loadBalancer"

	desc """绑定负载均衡器服务器组到监听器"""

	rest {
		request {
			url "POST /v1/load-balancers/listeners/{listenerUuid}/servergroups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddServerGroupToLoadBalancerListenerMsg.class

			desc """"""

			params {

				column {
					name "serverGroupUuid"
					enclosedIn "params"
					desc "负载均衡器服务器组uuid"
					location "body"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "listenerUuid"
					enclosedIn "params"
					desc "负载均衡器监听器uuid"
					location "url"
					type "String"
					optional false
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
			clz APIAddServerGroupToLoadBalancerListenerEvent.class
		}
	}
}