package org.zstack.pciDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.PciDeviceInventory

doc {

	title "PCI设备查询结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.APIQueryPciDeviceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.pciDevice.APIQueryPciDeviceReply.inventories"
		desc "PCI设备清单列表"
		type "List"
		since "2.1"
		clz PciDeviceInventory.class
	}
}
