package org.zstack.header.host

import org.zstack.header.host.HostInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "关闭物理机消息回复"

	ref {
		name "inventory"
		path "org.zstack.header.host.APIShutdownHostEvent.inventory"
		desc "物理机信息"
		type "HostInventory"
		since "zsv 3.17.0"
		clz HostInventory.class
	}
	field {
		name "success"
		desc "是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIShutdownHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
