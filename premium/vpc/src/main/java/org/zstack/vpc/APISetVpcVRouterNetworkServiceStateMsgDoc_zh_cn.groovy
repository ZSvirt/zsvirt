package org.zstack.vpc

import org.zstack.vpc.APISetVpcVRouterNetworkServiceStateEvent

doc {
	title "SetVpcVRouterNetworkServiceState"

	category "vpc"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/vpc/virtual-routers/{uuid}/networkservicestate"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVpcVRouterNetworkServiceStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "networkService"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("SNAT")
				}
				column {
					name "state"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable")
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional true
					since "3.13.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.13.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.13.6"
				}
			}
		}

		response {
			clz APISetVpcVRouterNetworkServiceStateEvent.class
		}
	}
}