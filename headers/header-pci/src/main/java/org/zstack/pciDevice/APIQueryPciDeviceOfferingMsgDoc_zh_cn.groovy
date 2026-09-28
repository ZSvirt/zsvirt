package org.zstack.pciDevice

import org.zstack.pciDevice.APIQueryPciDeviceOfferingReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "查询PCI设备规格"

	category "pciDevice"

	desc """查询PCI设备规格"""

	rest {
		request {
			url "GET /v1/pci-device/pci-device-offerings"
			url "GET /v1/pci-device/pci-device-offerings/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPciDeviceOfferingMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPciDeviceOfferingReply.class
		}
	}
}