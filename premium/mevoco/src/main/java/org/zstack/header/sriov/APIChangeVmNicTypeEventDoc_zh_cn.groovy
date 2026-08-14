package org.zstack.header.sriov

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmNicInventory

doc {

	title "修改云主机网卡类型返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.sriov.APIChangeVmNicTypeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.sriov.APIChangeVmNicTypeEvent.inventory"
		desc "云主机网卡清单"
		type "VmNicInventory"
		since "3.9.0"
		clz VmNicInventory.class
	}
}
