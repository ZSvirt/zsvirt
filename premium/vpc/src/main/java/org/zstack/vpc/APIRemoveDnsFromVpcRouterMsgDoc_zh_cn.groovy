package org.zstack.vpc

import org.zstack.vpc.APIRemoveDnsFromVpcRouterEvent

doc {
	title "RemoveDnsFromVpcRouter"

	category "vpc"

	desc """从VPC云路由移除DNS"""

	rest {
		request {
			url "DELETE /v1/vpc/virtual-routers/{uuid}/dns"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveDnsFromVpcRouterMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.4"
				}
				column {
					name "dns"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "2.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.4"
				}
			}
		}

		response {
			clz APIRemoveDnsFromVpcRouterEvent.class
		}
	}
}