package org.zstack.network.service.flat

import org.zstack.network.service.flat.APIChangeL3NetworkDhcpIpAddressEvent

doc {
	title "ChangeL3NetworkDhcpIpAddress"

	category "flat.dhcp"

	desc """修改DHCP服务器地址"""

	rest {
		request {
			url "PUT /v1/l3-networks/{l3NetworkUuid}/dhcp-ip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeL3NetworkDhcpIpAddressMsg.class

			desc """"""

			params {

				column {
					name "l3NetworkUuid"
					enclosedIn "changeL3NetworkDhcpIpAddress"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "4.10.16"
				}
				column {
					name "dhcpServerIp"
					enclosedIn "changeL3NetworkDhcpIpAddress"
					desc "DHCP v4 服务器地址"
					location "body"
					type "String"
					optional true
					since "4.10.16"
				}
				column {
					name "dhcpv6ServerIp"
					enclosedIn "changeL3NetworkDhcpIpAddress"
					desc "DHCP v6 服务器地址"
					location "body"
					type "String"
					optional true
					since "4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.16"
				}
			}
		}

		response {
			clz APIChangeL3NetworkDhcpIpAddressEvent.class
		}
	}
}