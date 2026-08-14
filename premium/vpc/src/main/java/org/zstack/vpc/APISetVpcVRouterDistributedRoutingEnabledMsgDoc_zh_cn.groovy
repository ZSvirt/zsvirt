package org.zstack.vpc

import org.zstack.vpc.APISetVpcVRouterDistributedRoutingEnabledEvent

doc {
	title "SetVpcVRouterDistributedRoutingEnabled"

	category "vpc"

	desc """设置VPC云路由分布式路由开关"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers/{uuid}/distributed-routing"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVpcVRouterDistributedRoutingEnabledMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "stateEvent"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "2.3"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APISetVpcVRouterDistributedRoutingEnabledEvent.class
		}
	}
}