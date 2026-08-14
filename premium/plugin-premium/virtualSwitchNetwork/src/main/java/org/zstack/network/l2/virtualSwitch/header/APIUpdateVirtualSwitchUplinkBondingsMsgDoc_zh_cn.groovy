package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.APIUpdateVirtualSwitchUplinkBondingsEvent

doc {
	title "UpdateVirtualSwitchUplinkBondings"

	category "network.l2"

	desc """更新交换机上行链路绑定配置"""

	rest {
		request {
			url "PUT /v1/l2-networks/virtual-switch/{uuid}/uplink-bondings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateVirtualSwitchUplinkBondingsMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateVirtualSwitchUplinkBondings"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.18.0"
				}
				column {
					name "mode"
					enclosedIn "updateVirtualSwitchUplinkBondings"
					desc "绑定模式"
					location "body"
					type "String"
					optional false
					since "3.18.0"
					values ("802.3ad","active-backup")
				}
				column {
					name "xmitHashPolicy"
					enclosedIn "updateVirtualSwitchUplinkBondings"
					desc "哈希策略"
					location "body"
					type "String"
					optional true
					since "3.18.0"
					values ("layer2","layer2+3","layer3+4")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "bondingName"
					enclosedIn "updateVirtualSwitchUplinkBondings"
					desc "绑定名称"
					location "body"
					type "String"
					optional true
					since "3.13.0"
				}
			}
		}

		response {
			clz APIUpdateVirtualSwitchUplinkBondingsEvent.class
		}
	}
}