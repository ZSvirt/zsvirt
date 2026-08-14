package org.zstack.pciDevice.gpu

import org.zstack.pciDevice.gpu.GpuDeviceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询 GPU 设备返回"

	ref {
		name "inventories"
		path "org.zstack.pciDevice.gpu.APIQueryGpuDeviceReply.inventories"
		desc "GPU 设备清单列表"
		type "List"
		since "4.10.6"
		clz GpuDeviceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.6"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.gpu.APIQueryGpuDeviceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.6"
		clz ErrorCode.class
	}
}
