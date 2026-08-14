package org.zstack.network.service.lb

import org.zstack.network.service.lb.APIGetCandidateL3NetworksForServerGroupReply

doc {
	title "GetCandidateL3NetworksForServerGroup"

	category "loadBalancer"

	desc """查询负载均衡后端服务组所能绑定的三层网络"""

	rest {
		request {
			url "GET /v1/load-balancers/servergroups/candidate-l3network"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateL3NetworksForServerGroupMsg.class

			desc """"""

			params {

				column {
					name "serverGroupUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.13.0"
				}
				column {
					name "loadBalancerUuid"
					enclosedIn ""
					desc "负载均衡器UUID"
					location "query"
					type "String"
					optional true
					since "3.13.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.13.0"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.13.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.13.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.13.0"
				}
			}
		}

		response {
			clz APIGetCandidateL3NetworksForServerGroupReply.class
		}
	}
}