package org.zstack.crypto.keyprovider.api

import org.zstack.header.keyprovider.KeyProviderInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询密钥提供程序结果"

	ref {
		name "inventories"
		path "org.zstack.crypto.keyprovider.api.APIQueryKeyProviderReply.inventories"
		desc "密钥提供程序清单"
		type "List"
		since "5.0.0"
		clz KeyProviderInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.api.APIQueryKeyProviderReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
