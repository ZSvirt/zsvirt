package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.header.keyprovider.NkpInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "导入原生密钥提供程序结果"

	ref {
		name "inventory"
		path "org.zstack.crypto.keyprovider.nkp.api.APIRestoreNkpEvent.inventory"
		desc "原生密钥提供程序"
		type "NkpInventory"
		since "5.0.0"
		clz NkpInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.nkp.api.APIRestoreNkpEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
