package org.zstack.network.service.lb

import org.zstack.network.service.lb.APICreateLoadBalancerServerGroupEvent

doc {
	title "CreateLoadBalancerServerGroup"

	category "loadBalancer"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/load-balancers/{loadBalancerUuid}/servergroups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateLoadBalancerServerGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "loadBalancerUuid"
					enclosedIn "params"
					desc "负载均衡器UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
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
			clz APICreateLoadBalancerServerGroupEvent.class
		}
	}
}