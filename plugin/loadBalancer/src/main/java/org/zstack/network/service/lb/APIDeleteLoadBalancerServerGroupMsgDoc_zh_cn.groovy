package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIDeleteLoadBalancerServerGroupEvent

doc {
	title "DeleteLoadBalancerServerGroup"

	category "loadBalancer"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/load-balancers/servergroups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteLoadBalancerServerGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
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
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteLoadBalancerServerGroupEvent.class
		}
	}
}