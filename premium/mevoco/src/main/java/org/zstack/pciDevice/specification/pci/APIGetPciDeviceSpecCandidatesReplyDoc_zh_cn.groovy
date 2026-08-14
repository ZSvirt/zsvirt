package org.zstack.pciDevice.specification.pci

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.specification.pci.PciDeviceSpecInventory

doc {

	title "获取PCI设备规格列表结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.specification.pci.APIGetPciDeviceSpecCandidatesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.pciDevice.specification.pci.APIGetPciDeviceSpecCandidatesReply.inventories"
		desc "PCI设备规格列表"
		type "List"
		since "3.5.0"
		clz PciDeviceSpecInventory.class
	}
}
