package org.zstack.pciDevice.specification.mdev

import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新MDEV设备规格的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.specification.mdev.APIUpdateMdevDeviceSpecEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.pciDevice.specification.mdev.APIUpdateMdevDeviceSpecEvent.inventory"
		desc "更新后的MDEV设备规格"
		type "MdevDeviceSpecInventory"
		since "3.5.0"
		clz MdevDeviceSpecInventory.class
	}
}
