package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIPowerResetBaremetalChassisEvent

doc {
	title "PowerResetBaremetalChassis"

	category "baremetal.chassis"

	desc """重启裸机设备电源"""

	rest {
		request {
			url "PUT /v1/baremetal/chassis/{chassisUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPowerResetBaremetalChassisMsg.class

			desc """重启裸机设备电源"""

			params {

				column {
					name "chassisUuid"
					enclosedIn "powerResetBaremetalChassis"
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
			clz APIPowerResetBaremetalChassisEvent.class
		}
	}
}