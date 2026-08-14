package org.zstack.header.host

import org.zstack.header.host.APILocateHostNetworkInterfaceEvent

doc {
	title "LocateHostNetworkInterface"

	category "host"

	desc """物理网卡定位"""

	rest {
		request {
			url "PUT /v1/hosts/{hostUuid}/locate/network-interface"

			header (Authorization: 'OAuth the-session-uuid')

			clz APILocateHostNetworkInterfaceMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn "locateHostNetworkInterface"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "3.12.0"
				}
				column {
					name "networkInterfaceName"
					enclosedIn "locateHostNetworkInterface"
					desc "网卡名称"
					location "body"
					type "String"
					optional false
					since "3.12.0"
				}
				column {
					name "interval"
					enclosedIn "locateHostNetworkInterface"
					desc "定位灯亮的时间，单位秒"
					location "body"
					type "Long"
					optional true
					since "3.12.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.12.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.12.0"
				}
			}
		}

		response {
			clz APILocateHostNetworkInterfaceEvent.class
		}
	}
}