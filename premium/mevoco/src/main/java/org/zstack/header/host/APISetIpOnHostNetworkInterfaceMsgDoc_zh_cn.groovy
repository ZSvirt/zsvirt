package org.zstack.header.host

import org.zstack.header.host.APISetIpOnHostNetworkInterfaceEvent

doc {
	title "SetIpOnHostNetworkInterface"

	category "host"

	desc """在物理网口配置ip"""

	rest {
		request {
			url "POST /v1/hosts/nics/{interfaceUuid}/ip"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetIpOnHostNetworkInterfaceMsg.class

			desc """"""

			params {

				column {
					name "interfaceUuid"
					enclosedIn "params"
					desc "物理网口Uuid"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "ipAddress"
					enclosedIn "params"
					desc "IP地址"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "netmask"
					enclosedIn "params"
					desc "子网掩码"
					location "body"
					type "String"
					optional true
					since "3.17.0"
				}
				column {
					name "isDefaultRoute"
					enclosedIn "params"
					desc "是否设置默认路由"
					location "body"
					type "Boolean"
					optional true
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APISetIpOnHostNetworkInterfaceEvent.class
		}
	}
}