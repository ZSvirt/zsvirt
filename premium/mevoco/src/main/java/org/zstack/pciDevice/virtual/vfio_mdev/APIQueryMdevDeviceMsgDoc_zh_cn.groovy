package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.vfio_mdev.APIQueryMdevDeviceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryMdevDevice"

	category "pciDevice"

	desc """查询PCI设备切分出的MDEV设备"""

	rest {
		request {
			url "GET /v1/mdev-devices"
			url "GET /v1/mdev-devices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryMdevDeviceMsg.class

			desc """查询PCI设备切分出的MDEV设备"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryMdevDeviceReply.class
		}
	}
}