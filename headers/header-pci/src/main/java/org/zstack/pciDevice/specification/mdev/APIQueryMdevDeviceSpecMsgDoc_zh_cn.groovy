package org.zstack.pciDevice.specification.mdev

import org.zstack.pciDevice.specification.mdev.APIQueryMdevDeviceSpecReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMdevDeviceSpec"

	category "pciDevice"

	desc """查询PCI设备切分出的MDEV设备"""

	rest {
		request {
			url "GET /v1/mdev-device-specs"
			url "GET /v1/mdev-device-specs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMdevDeviceSpecMsg.class

			desc """查询PCI设备切分出的MDEV设备"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMdevDeviceSpecReply.class
		}
	}
}