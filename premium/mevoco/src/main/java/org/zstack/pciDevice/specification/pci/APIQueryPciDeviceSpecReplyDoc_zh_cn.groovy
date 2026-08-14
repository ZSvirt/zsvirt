package org.zstack.pciDevice.specification.pci

import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询PCI设备规格结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.specification.pci.APIQueryPciDeviceSpecReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.pciDevice.specification.pci.APIQueryPciDeviceSpecReply.inventories"
		desc "PCI设备规格清单"
		type "List"
		since "3.5.0"
		clz PciDeviceSpecInventory.class
	}
}
