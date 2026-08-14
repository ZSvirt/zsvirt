package org.zstack.header.bonding

import org.zstack.header.bonding.APIDetachNicFromBondingEvent

doc {
	title "DetachNicFromBonding"

	category "bonding"

	desc """从Bond网口解绑slave"""

	rest {
		request {
			url "PUT /v1/hosts/bondings/{uuid}/detach"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachNicFromBondingMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "detachNicFromBonding"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.17.11"
				}
				column {
					name "slaveUuids"
					enclosedIn "detachNicFromBonding"
					desc "slave网卡Uuids"
					location "body"
					type "List"
					optional false
					since "3.17.11"
				}
				column {
					name "type"
					enclosedIn "detachNicFromBonding"
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
			clz APIDetachNicFromBondingEvent.class
		}
	}
}