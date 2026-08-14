package org.zstack.pciDevice.specification.pci

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.specification.pci.VmInstancePciDeviceSpecRefInventory

doc {

	title "查询云主机与PCI设备规格关联关系的返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.specification.pci.APIQueryVmInstancePciDeviceSpecRefReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.pciDevice.specification.pci.APIQueryVmInstancePciDeviceSpecRefReply.inventories"
		desc "云主机与PCI设备规格的关联关系"
		type "List"
		since "3.5.0"
		clz VmInstancePciDeviceSpecRefInventory.class
	}
}
