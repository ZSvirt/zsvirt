package org.zstack.pciDevice.specification.pci

import org.zstack.pciDevice.specification.pci.APIQueryVmInstancePciDeviceSpecRefReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryVmInstancePciDeviceSpecRef"

	category "pciDevice"

	desc """查询云主机与PCI设备规格的关联关系"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/pci-device-specs"
			url "GET /v1/vm-instances/{vmInstanceUuid}/pci-device-specs/{pciSpecUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryVmInstancePciDeviceSpecRefMsg.class

			desc """查询云主机与PCI设备规格的关联关系"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryVmInstancePciDeviceSpecRefReply.class
		}
	}
}