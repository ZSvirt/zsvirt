package org.zstack.network.hostNetworkInterface.lldp.api

import org.zstack.network.hostNetworkInterface.lldp.api.APIGetHostNetworkInterfaceLldpReply

doc {
	title "GetHostNetworkInterfaceLldp"

	category "hostNetwork.lldp"

	desc """获取物理网口lldp信息"""

	rest {
		request {
			url "GET /v1/hostNetworkInterface/lldp/{interfaceUuid}/info"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostNetworkInterfaceLldpMsg.class

			desc """"""

			params {

				column {
					name "interfaceUuid"
					enclosedIn ""
					desc "物理网口Uuid"
					location "url"
					type "String"
					optional false
					since "4.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.1.0"
				}
			}
		}

		response {
			clz APIGetHostNetworkInterfaceLldpReply.class
		}
	}
}