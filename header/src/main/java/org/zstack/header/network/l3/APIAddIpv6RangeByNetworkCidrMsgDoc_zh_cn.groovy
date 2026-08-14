package org.zstack.header.network.l3

import org.zstack.header.network.l3.APIAddIpRangeByNetworkCidrEvent

doc {
	title "通过网络CIDR添加IPv6地址范围(AddIpv6RangeByNetworkCidr)"

	category "三层网络"

	desc """通过网络CIDR添加IPv6地址范围"""

	rest {
		request {
			url "POST /v1/l3-networks/{l3NetworkUuid}/ipv6-ranges/by-cidr"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddIpv6RangeByNetworkCidrMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "三层网络的名称"
					location "body"
					type "String"
					optional false
					since "zsv 3.1"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "三层网络的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.1"
				}
				column {
					name "l3NetworkUuid"
					enclosedIn "params"
					desc "三层网络UUID"
					location "url"
					type "String"
					optional false
					since "zsv 3.1"
				}
				column {
					name "networkCidr"
					enclosedIn "params"
					desc "网络CIDR"
					location "body"
					type "String"
					optional false
					since "zsv 3.1"
				}
				column {
					name "addressMode"
					enclosedIn "params"
					desc "IPv6地址分配模式"
					location "body"
					type "String"
					optional false
					since "zsv 3.1"
					values ("SLAAC","Stateful-DHCP","Stateless-DHCP")
				}
				column {
					name "ipRangeType"
					enclosedIn "params"
					desc "地址段类型"
					location "body"
					type "String"
					optional true
					since "zsv 3.9"
					values ("Normal","AddressPool")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，三层网络会使用该字段值作为UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.1"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
			}
		}

		response {
			clz APIAddIpRangeByNetworkCidrEvent.class
		}
	}
}