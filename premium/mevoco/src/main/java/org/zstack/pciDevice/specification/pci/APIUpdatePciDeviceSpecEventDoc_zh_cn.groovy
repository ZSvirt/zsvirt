package org.zstack.pciDevice.specification.pci

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.specification.pci.PciDeviceSpecInventory

doc {

	title "更新PCI设备规格结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.specification.pci.APIUpdatePciDeviceSpecEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.pciDevice.specification.pci.APIUpdatePciDeviceSpecEvent.inventory"
		desc "更新后的PCI设备规格清单"
		type "PciDeviceSpecInventory"
		since "3.5.0"
		clz PciDeviceSpecInventory.class
	}
}
