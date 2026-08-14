package org.zstack.vpc

import org.zstack.vpc.APIGetRouteTableVpcVRouterCandidateReply

doc {
	title "GetRouteTableVpcVRouterCandidate"

	category "vpc"

	desc """创建路由表时提供可用的VPC路由器列表"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers/get-vpc-candidate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetRouteTableVpcVRouterCandidateMsg.class

			desc """"""

			params {

				column {
					name "tableUuid"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.12.0"
				}
				column {
					name "limit"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.12.0"
				}
				column {
					name "start"
					enclosedIn ""
					desc ""
					location "query"
					type "Integer"
					optional true
					since "3.12.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.12.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.12.0"
				}
			}
		}

		response {
			clz APIGetRouteTableVpcVRouterCandidateReply.class
		}
	}
}