package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.HostKernelInterfaceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建主机网络适配器的请求结果"

	ref {
		name "inventory"
		path "org.zstack.network.l2.virtualSwitch.header.APICreateHostKernelInterfaceEvent.inventory"
		desc "主机网络适配器清单"
		type "HostKernelInterfaceInventory"
		since "4.1.0"
		clz HostKernelInterfaceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.1.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APICreateHostKernelInterfaceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.1.0"
		clz ErrorCode.class
	}
}
