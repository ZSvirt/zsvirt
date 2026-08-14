package org.zstack.core.config

import org.zstack.header.errorcode.ErrorCode
import org.zstack.core.config.GlobalConfigInventory

doc {

	title "更新全局配置返回信息"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.core.config.APIUpdateGlobalConfigEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.core.config.APIUpdateGlobalConfigEvent.inventory"
		desc "null"
		type "GlobalConfigInventory"
		since "0.6"
		clz GlobalConfigInventory.class
	}
}
