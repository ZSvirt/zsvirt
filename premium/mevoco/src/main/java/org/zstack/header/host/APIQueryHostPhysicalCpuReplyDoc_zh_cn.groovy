package org.zstack.header.host

import org.zstack.header.host.HostPhysicalCpuInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询物理机CPU信息的返回"

	ref {
		name "inventories"
		path "org.zstack.header.host.APIQueryHostPhysicalCpuReply.inventories"
		desc "物理机CPU信息"
		type "List"
		since "zsv 4.10.0"
		clz HostPhysicalCpuInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIQueryHostPhysicalCpuReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.0"
		clz ErrorCode.class
	}
}
