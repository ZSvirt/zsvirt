package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIRemoveVmNicFromLoadBalancerEvent

doc {
	title "从负载均衡移除云主机网卡(RemoveVmNicFromLoadBalancer)"

	category "负载均衡"

	desc """从负载均衡移除云主机网卡"""

	rest {
		request {
			url "DELETE /v1/load-balancers/listeners/{listenerUuid}/vm-instances/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveVmNicFromLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuids"
					enclosedIn ""
					desc "云主机网卡UUID"
					location "query"
					type "List"
					optional false
					since "0.6"
				}
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
			clz APIRemoveVmNicFromLoadBalancerEvent.class
		}
	}
}