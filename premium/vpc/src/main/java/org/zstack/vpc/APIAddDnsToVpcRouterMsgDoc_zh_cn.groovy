package org.zstack.vpc

import org.zstack.vpc.APIAddDnsToVpcRouterEvent

doc {
	title "AddDnsToVpcRouter"

	category "vpc"

	desc """向VPC云路由添加DNS"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers/{uuid}/dns"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddDnsToVpcRouterMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.4"
				}
				column {
					name "dns"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "2.4"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.4"
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
			}
		}

		response {
			clz APIAddDnsToVpcRouterEvent.class
		}
	}
}