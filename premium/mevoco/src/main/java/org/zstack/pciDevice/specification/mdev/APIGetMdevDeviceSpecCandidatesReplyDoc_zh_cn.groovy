package org.zstack.pciDevice.specification.mdev

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecInventory

doc {

	title "获取可用的MDEV设备规格的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.specification.mdev.APIGetMdevDeviceSpecCandidatesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.pciDevice.specification.mdev.APIGetMdevDeviceSpecCandidatesReply.inventories"
		desc "可用MDEV设备规格列表"
		type "List"
		since "3.5.0"
		clz MdevDeviceSpecInventory.class
	}
}
