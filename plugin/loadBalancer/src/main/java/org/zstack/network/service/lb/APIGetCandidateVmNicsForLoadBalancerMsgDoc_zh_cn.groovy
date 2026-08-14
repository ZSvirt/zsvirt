package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetCandidateVmNicsForLoadBalancerReply

doc {
	title "获取可供负载均衡器添加的云主机网卡(GetCandidateVmNicsForLoadBalancer)"

	category "负载均衡"

	desc """获取可供负载均衡器添加的云主机网卡"""

	rest {
		request {
			url "GET /v1/load-balancers/listeners/{listenerUuid}/vm-instances/candidate-nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateVmNicsForLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "listenerUuid"
					enclosedIn ""
					desc "负载均衡监听器UUID"
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
			clz APIGetCandidateVmNicsForLoadBalancerReply.class
		}
	}
}