package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIInspectBaremetalChassisEvent

doc {
	title "InspectBaremetalChassis"

	category "baremetal.chassis"

	desc """下发获取裸机硬件信息命令"""

	rest {
		request {
			url "PUT /v1/baremetal/chassis/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIInspectBaremetalChassisMsg.class

			desc """下发获取裸机硬件信息命令"""

			params {

				column {
					name "uuid"
					enclosedIn "inspectBaremetalChassis"
					desc "裸机设备UUID"
					location "url"
					type "String"
					optional false
					since "2.6.0"
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
			clz APIInspectBaremetalChassisEvent.class
		}
	}
}