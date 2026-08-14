package org.zstack.vpc

import org.zstack.vpc.APIGetVpcVRouterDistributedRoutingConnectionsReply

doc {
	title "GetVpcVRouterDistributedRoutingConnections"

	category "vpc"

	desc """获取VPC云路由实时流量状态"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers/{uuid}/tracked-connections"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVpcVRouterDistributedRoutingConnectionsMsg.class

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
			clz APIGetVpcVRouterDistributedRoutingConnectionsReply.class
		}
	}
}