package org.zstack.crypto.keyprovider.kms.api

import org.zstack.header.keyprovider.KeyProviderInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新KMS密钥提供程序结果"

	ref {
		name "inventory"
		path "org.zstack.crypto.keyprovider.kms.api.APIUpdateKmsEvent.inventory"
		desc "KMS密钥提供程序"
		type "KeyProviderInventory"
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
		path "org.zstack.crypto.keyprovider.kms.api.APIUpdateKmsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
