package org.zstack.kvm.hypervisor.message

import org.zstack.kvm.hypervisor.datatype.HostOsCategoryInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询主机系统类型结果"

	ref {
		name "inventories"
		path "org.zstack.kvm.hypervisor.message.APIQueryHostOsCategoryReply.inventories"
		desc "主机系统类型清单列表"
		type "List"
		since "3.16.21"
		clz HostOsCategoryInventory.class
	}
	field {
		name "success"
		desc "查询是否成功"
		type "boolean"
		since "3.16.21"
	}
	ref {
		name "error"
		path "org.zstack.kvm.hypervisor.message.APIQueryHostOsCategoryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.21"
		clz ErrorCode.class
	}
}
