package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostPhysicalMemoryInventory

doc {

	title "获取物理机内存卡信息清单"

	ref {
		name "inventories"
		path "org.zstack.header.host.APIGetHostPhysicalMemoryFactsReply.inventories"
		desc "物理机内存卡清单列表"
		type "List"
		since "3.12.0"
		clz HostPhysicalMemoryInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.12.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostPhysicalMemoryFactsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.12.0"
		clz ErrorCode.class
	}
}
