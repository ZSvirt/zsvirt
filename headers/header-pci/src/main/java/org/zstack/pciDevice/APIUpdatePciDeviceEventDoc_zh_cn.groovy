package org.zstack.pciDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.entity.PciDeviceInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.APIUpdatePciDeviceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.pciDevice.APIUpdatePciDeviceEvent.inventory"
		desc "null"
		type "PciDeviceInventory"
		since "zsv_2.1"
		clz PciDeviceInventory.class
	}
}
