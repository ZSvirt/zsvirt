package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIPowerOffBaremetalChassisEvent

doc {
	title "PowerOffBaremetalChassis"

	category "baremetal.chassis"

	desc """关闭裸机设备电源"""

	rest {
		request {
			url "PUT /v1/baremetal/chassis/{chassisUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIPowerOffBaremetalChassisMsg.class

			desc """关闭裸机设备电源"""

			params {

				column {
					name "chassisUuid"
					enclosedIn "powerOffBaremetalChassis"
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
			clz APIPowerOffBaremetalChassisEvent.class
		}
	}
}