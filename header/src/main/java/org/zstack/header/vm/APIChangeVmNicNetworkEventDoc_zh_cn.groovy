package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmNicInventory

doc {

	title "修改虚拟机网卡的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.11.0"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIChangeVmNicNetworkEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.11.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.vm.APIChangeVmNicNetworkEvent.inventory"
		desc "虚拟机网卡清单"
		type "VmNicInventory"
		since "zsv 3.11.0"
		clz VmNicInventory.class
	}
}
