package org.zstack.pciDevice

import org.zstack.pciDevice.APIQueryPciDevicePciDeviceOfferingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询PCI设备规格匹配"

	category "pciDevice"

	desc """查询PCI设备规格匹配"""

	rest {
		request {
			url "GET /v1/pci-devices/pci-devices/pci-device-offerings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPciDevicePciDeviceOfferingMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPciDevicePciDeviceOfferingReply.class
		}
	}
}