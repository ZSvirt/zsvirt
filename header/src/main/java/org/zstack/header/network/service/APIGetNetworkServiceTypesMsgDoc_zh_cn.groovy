package org.zstack.header.network.service

import org.zstack.header.network.service.APIGetNetworkServiceTypesReply

doc {
	title "获取网络服务类型(GetNetworkServiceTypes)"

	category "network.service"

	desc """获取网络服务类型"""

	rest {
		request {
			url "GET /v1/network-services/types"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetNetworkServiceTypesMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetNetworkServiceTypesReply.class
		}
	}
}