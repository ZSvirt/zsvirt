package org.zstack.header.vm

import org.zstack.header.vm.APISetVmStaticIpEvent

doc {
	title "指定云主机IP(SetVmStaticIp)"

	category "vmInstance"

	desc """给云主机网卡指定IP，用户可以通过该API控制ZStack分配给云主机网卡的IP。

用户要确保指定的IP在指定三层网络，并且IP未被占用。
"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmStaticIpMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "setVmStaticIp"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "setVmStaticIp"
					desc "三层网络UUID"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "ip"
					enclosedIn "setVmStaticIp"
					desc "指定IP地址"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "ip6"
					enclosedIn "setVmStaticIp"
					desc "指定IPv6地址"
					location "body"
					type "String"
					optional true
					since "zsv 3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "netmask"
					enclosedIn "setVmStaticIp"
					desc "IPv4子网掩码"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "gateway"
					enclosedIn "setVmStaticIp"
					desc "IPv4网关"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "ipv6Gateway"
					enclosedIn "setVmStaticIp"
					desc "IPv6网关"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "ipv6Prefix"
					enclosedIn "setVmStaticIp"
					desc "IPv6前缀长度"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APISetVmStaticIpEvent.class
		}
	}
}