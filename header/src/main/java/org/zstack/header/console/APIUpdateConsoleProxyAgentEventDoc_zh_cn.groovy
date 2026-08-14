package org.zstack.header.console

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.console.ConsoleProxyAgentInventory

doc {

	title "更新控制台代理的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.console.APIUpdateConsoleProxyAgentEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.console.APIUpdateConsoleProxyAgentEvent.inventory"
		desc "更新后的控制台代理"
		type "ConsoleProxyAgentInventory"
		since "zsv 2.3"
		clz ConsoleProxyAgentInventory.class
	}
}
