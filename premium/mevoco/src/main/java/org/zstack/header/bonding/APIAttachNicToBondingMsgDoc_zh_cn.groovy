package org.zstack.header.bonding

import org.zstack.header.bonding.APIAttachNicToBondingEvent

doc {
	title "AttachNicToBonding"

	category "bonding"

	desc """绑定slave到Bond网口"""

	rest {
		request {
			url "PUT /v1/hosts/bondings/{uuid}/attach"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachNicToBondingMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "attachNicToBonding"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "slaveUuids"
					enclosedIn "attachNicToBonding"
					desc "slave网卡Uuids"
					location "body"
					type "List"
					optional false
					since "3.17.11"
				}
				column {
					name "type"
					enclosedIn "attachNicToBonding"
					desc "Bond类型"
					location "body"
					type "String"
					optional true
					since "3.17.11"
					values ("LinuxBonding","OvsBonding")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.17.11"
				}
			}
		}

		response {
			clz APIAttachNicToBondingEvent.class
		}
	}
}