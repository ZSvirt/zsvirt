package org.zstack.pciDevice.specification.pci

import org.zstack.pciDevice.specification.pci.APIQueryPciDeviceSpecReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryPciDeviceSpec"

	category "pciDevice"

	desc """查询PCI设备规格"""

	rest {
		request {
			url "GET /v1/pci-device-specs"
			url "GET /v1/pci-device-specs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryPciDeviceSpecMsg.class

			desc """查询PCI设备规格"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryPciDeviceSpecReply.class
		}
	}
}