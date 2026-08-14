package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIAddVmNicToLoadBalancerEvent

doc {
	title "添加云主机网卡到负载均衡(AddVmNicToLoadBalancer)"

	category "负载均衡"

	desc """添加云主机网卡到负载均衡"""

	rest {
		request {
			url "POST /v1/load-balancers/listeners/{listenerUuid}/vm-instances/nics"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddVmNicToLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "vmNicUuids"
					enclosedIn "params"
					desc "云主机网卡UUID"
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "listenerUuid"
					enclosedIn "params"
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
			clz APIAddVmNicToLoadBalancerEvent.class
		}
	}
}