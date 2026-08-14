package org.zstack.header.host

import org.zstack.header.host.APIGetHostNetworkFactsReply

doc {
	title "GetHostNetworkFacts"

	category "host"

	desc """获取物理机物理网络信息"""

	rest {
		request {
			url "GET /v1/hosts/network-facts/{hostUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostNetworkFactsMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APIGetHostNetworkFactsReply.class
		}
	}
}