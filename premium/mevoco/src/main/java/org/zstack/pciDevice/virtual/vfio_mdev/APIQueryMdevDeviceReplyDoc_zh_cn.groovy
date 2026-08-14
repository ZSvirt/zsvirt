package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.header.errorcode.ErrorCode

doc {

	title "PCI设备切分出的MDEV设备列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.virtual.vfio_mdev.APIQueryMdevDeviceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.pciDevice.virtual.vfio_mdev.APIQueryMdevDeviceReply.inventories"
		desc "MDEV设备清单"
		type "List"
		since "3.5.0"
		clz MdevDeviceInventory.class
	}
}
