package org.zstack.network.service.flat

import org.zstack.network.service.flat.APIGetL3NetworkDhcpIpAddressReply

doc {
	title "获取网络DHCP服务所用地址(GetL3NetworkDhcpIpAddress)"

	category "三层网络"

	desc """获取网络DHCP服务所用地址"""

	rest {
		request {
			url "GET /v1/l3-networks/{l3NetworkUuid}/dhcp-ip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetL3NetworkDhcpIpAddressMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn ""
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetL3NetworkDhcpIpAddressReply.class
		}
	}
}