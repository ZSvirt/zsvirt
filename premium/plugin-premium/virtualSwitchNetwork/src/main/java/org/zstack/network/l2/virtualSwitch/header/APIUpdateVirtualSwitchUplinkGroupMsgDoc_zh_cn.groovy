package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIUpdateVirtualSwitchUplinkGroupEvent

doc {
	title "更新交换机上行链路组(UpdateVirtualSwitchUplinkGroup)"

	category "network.l2"

	desc """更新交换机上行链路组"""

	rest {
		request {
			url "PUT /v1/l2-networks/virtual-switch/{uuid}/uplink-group"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVirtualSwitchUplinkGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVirtualSwitchUplinkGroup"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "hostUuid"
					enclosedIn "updateVirtualSwitchUplinkGroup"
					desc "物理机UUID"
					location "body"
					type "String"
					optional false
					since "4.3.0"
				}
				column {
					name "slaveUuids"
					enclosedIn "updateVirtualSwitchUplinkGroup"
					desc "slave网卡UUID列表"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "slaveNames"
					enclosedIn "updateVirtualSwitchUplinkGroup"
					desc "slave网卡名称列表"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "type"
					enclosedIn "updateVirtualSwitchUplinkGroup"
					desc "绑定类型"
					location "body"
					type "String"
					optional true
					since "4.3.0"
					values ("LinuxBonding","OvsBonding")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.3.0"
				}
			}
		}

		response {
			clz APIUpdateVirtualSwitchUplinkGroupEvent.class
		}
	}
}