package org.zstack.pciDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.PciDeviceOfferingInventory

doc {

	title "PCI设备规格清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.APIQueryPciDeviceOfferingReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.pciDevice.APIQueryPciDeviceOfferingReply.inventories"
		desc "null"
		type "List"
		since "2.1"
		clz PciDeviceOfferingInventory.class
	}
}
