package org.zstack.header.vm

import org.zstack.header.vm.VmDnsInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "云主机DNS信息"

	ref {
		name "vmDnsList"
		path "org.zstack.header.vm.APIGetVmDnsReply.vmDnsList"
		desc "null"
		type "List"
		since "zsv 4.10.10"
		clz VmDnsInventory.class
	}
	field {
		name "dnsList"
		desc "DNS列表"
		type "List"
		since "zsv 4.10.10"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.10"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetVmDnsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.10"
		clz ErrorCode.class
	}
}
