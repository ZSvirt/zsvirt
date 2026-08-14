package org.zstack.pciDevice.specification.mdev

import org.zstack.header.errorcode.ErrorCode

doc {

	title "为云主机添加MDEV设备规格的返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.specification.mdev.APIAddMdevDeviceSpecToVmInstanceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.pciDevice.specification.mdev.APIAddMdevDeviceSpecToVmInstanceEvent.inventory"
		desc "新添加的云主机与MDEV设备规格的关联关系"
		type "VmInstanceMdevDeviceSpecRefInventory"
		since "3.5.0"
		clz VmInstanceMdevDeviceSpecRefInventory.class
	}
}
