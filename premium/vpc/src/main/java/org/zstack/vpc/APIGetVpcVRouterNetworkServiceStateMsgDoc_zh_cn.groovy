package org.zstack.vpc

import org.zstack.vpc.APIGetVpcVRouterNetworkServiceStateReply

doc {
	title "GetVpcVRouterNetworkServiceState"

	category "vpc"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vpc/virtual-routers/{uuid}/networkservicestate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetVpcVRouterNetworkServiceStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "networkService"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional false
					since "0.6"
					values ("SNAT")
				}
				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "query"
					type "String"
					optional true
					since "3.13.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetVpcVRouterNetworkServiceStateReply.class
		}
	}
}