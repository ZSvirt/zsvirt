package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIChangeBaremetalChassisStateEvent

doc {
	title "ChangeBaremetalChassisState"

	category "baremetal.chassis"

	desc """修改裸机设备状态"""

	rest {
		request {
			url "PUT /v1/baremetal/chassis/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeBaremetalChassisStateMsg.class

			desc """修改裸机设备状态"""

			params {

				column {
					name "uuid"
					enclosedIn "changeBaremetalChassisState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "stateEvent"
					enclosedIn "changeBaremetalChassisState"
					desc "状态事件"
					location "body"
					type "String"
					optional false
					since "2.6.0"
					values ("enable","disable")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIChangeBaremetalChassisStateEvent.class
		}
	}
}