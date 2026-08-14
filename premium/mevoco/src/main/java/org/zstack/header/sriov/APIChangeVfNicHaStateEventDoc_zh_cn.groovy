package org.zstack.header.sriov

import org.zstack.header.sriov.VmVfNicInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改云主机VF网卡高可用状态返回结果"

	ref {
		name "inventory"
		path "org.zstack.header.sriov.APIChangeVfNicHaStateEvent.inventory"
		desc "null"
		type "VmVfNicInventory"
		since "4.10.0"
		clz VmVfNicInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.sriov.APIChangeVfNicHaStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
}
