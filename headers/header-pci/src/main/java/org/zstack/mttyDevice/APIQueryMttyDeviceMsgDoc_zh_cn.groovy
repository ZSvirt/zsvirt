package org.zstack.mttyDevice

import org.zstack.mttyDevice.APIQueryMttyDeviceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMttyDevice"

	category "mttyDevice"

	desc """查询MTTY设备"""

	rest {
		request {
			url "GET /v1/mtty-devices"
			url "GET /v1/mtty-devices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMttyDeviceMsg.class

			desc """查询MTTY设备"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMttyDeviceReply.class
		}
	}
}