package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.header.keyprovider.NkpInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询原生密钥提供程序结果"

	ref {
		name "inventories"
		path "org.zstack.crypto.keyprovider.nkp.api.APIQueryNkpReply.inventories"
		desc "原生密钥提供程序清单"
		type "List"
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
		path "org.zstack.crypto.keyprovider.nkp.api.APIQueryNkpReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
