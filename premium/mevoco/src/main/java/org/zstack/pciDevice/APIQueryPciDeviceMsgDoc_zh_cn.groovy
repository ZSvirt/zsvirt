package org.zstack.pciDevice

import org.zstack.pciDevice.APIQueryPciDeviceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询PCI设备"

	category "pciDevice"

	desc """查询PCI设备"""

	rest {
		request {
			url "GET /v1/pci-device/pci-devices"
			url "GET /v1/pci-device/pci-devices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPciDeviceMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPciDeviceReply.class
		}
	}
}