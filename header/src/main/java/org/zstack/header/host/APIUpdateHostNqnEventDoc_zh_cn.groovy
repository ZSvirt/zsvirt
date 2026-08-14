package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新主机nqn消息回复"

	ref {
		name "inventory"
		path "org.zstack.header.host.APIUpdateHostNqnEvent.inventory"
		desc "null"
		type "HostInventory"
		since "zsv 4.10.6"
		clz HostInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.6"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIUpdateHostNqnEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.6"
		clz ErrorCode.class
	}
}
