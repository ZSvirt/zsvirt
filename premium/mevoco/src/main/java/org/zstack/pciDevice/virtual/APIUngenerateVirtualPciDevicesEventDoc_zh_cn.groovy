package org.zstack.pciDevice.virtual

import org.zstack.header.errorcode.ErrorCode

doc {

	title "PCI设备虚拟化还原结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
}
