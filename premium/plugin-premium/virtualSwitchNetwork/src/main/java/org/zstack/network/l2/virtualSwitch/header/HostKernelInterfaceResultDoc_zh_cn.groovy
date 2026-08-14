package org.zstack.network.l2.virtualSwitch.header

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceInventory

doc {

	title "Kernel适配器的返回结果"

	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceResult.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceResult.inventory"
		desc "null"
		type "HostKernelInterfaceInventory"
		since "4.10.20"
		clz HostKernelInterfaceInventory.class
	}
}
