package org.zstack.header.vm

import org.zstack.header.vm.VmDnsInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "虚拟机DNS清单列表"

	ref {
		name "inventories"
		path "org.zstack.header.vm.APISetVmDnsEvent.inventories"
		desc "null"
		type "List"
		since "zsv_4.10.10"
		clz VmDnsInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_4.10.10"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APISetVmDnsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_4.10.10"
		clz ErrorCode.class
	}
}
