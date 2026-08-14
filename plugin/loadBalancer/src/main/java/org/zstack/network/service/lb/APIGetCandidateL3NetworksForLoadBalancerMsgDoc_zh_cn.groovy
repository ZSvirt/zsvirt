package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetCandidateL3NetworksForLoadBalancerReply

doc {
	title "GetCandidateL3NetworksForLoadBalancer"

	category "loadBalancer"

	desc """获取监听器可加载L3网络"""

	rest {
		request {
			url "GET /v1/load-balancers/listeners/{listenerUuid}/networks/candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateL3NetworksForLoadBalancerMsg.class

			desc """"""

			params {

				column {
					name "listenerUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.9.0"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIGetCandidateL3NetworksForLoadBalancerReply.class
		}
	}
}