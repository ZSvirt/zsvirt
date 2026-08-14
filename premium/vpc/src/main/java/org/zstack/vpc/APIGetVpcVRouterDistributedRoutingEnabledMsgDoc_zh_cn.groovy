package org.zstack.vpc

import org.zstack.vpc.APIGetVpcVRouterDistributedRoutingEnabledReply

doc {
	title "GetVpcVRouterDistributedRoutingEnabled"

	category "vpc"

	desc """获取VPC云路由分布式路由是否打开"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers/{uuid}/distributed-routing"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVpcVRouterDistributedRoutingEnabledMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIGetVpcVRouterDistributedRoutingEnabledReply.class
		}
	}
}