package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIQueryBaremetalChassisReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryBaremetalChassis"

	category "baremetal.chassis"

	desc """查询裸机设备"""

	rest {
		request {
			url "GET /v1/baremetal/chassis"
			url "GET /v1/baremetal/chassis/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryBaremetalChassisMsg.class

			desc """查询裸机设备"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryBaremetalChassisReply.class
		}
	}
}