package org.zstack.network.hostNetworkInterface.lldp.api

import org.zstack.network.hostNetworkInterface.lldp.api.APIChangeHostNetworkInterfaceLldpModeEvent

doc {
	title "ChangeHostNetworkInterfaceLldpMode"

	category "hostNetwork.lldp"

	desc """修改lldp的工作模式"""

	rest {
		request {
			url "PUT /v1/hostNetworkInterface/lldp/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeHostNetworkInterfaceLldpModeMsg.class

			desc """"""

			params {

				column {
					name "interfaceUuids"
					enclosedIn "changeHostNetworkInterfaceLldpMode"
					desc "物理网口Uuids"
					location "body"
					type "List"
					optional false
					since "4.1.0"
				}
				column {
					name "mode"
					enclosedIn "changeHostNetworkInterfaceLldpMode"
					desc "lldp工作模式"
					location "body"
					type "String"
					optional true
					since "4.1.0"
					values ("rx_only","tx_only","rx_and_tx","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.1.0"
				}
			}
		}

		response {
			clz APIChangeHostNetworkInterfaceLldpModeEvent.class
		}
	}
}