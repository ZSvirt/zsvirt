package org.zstack.crypto.keyprovider.kms.api

import org.zstack.header.keyprovider.KmsInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询KMS密钥提供程序结果"

	ref {
		name "inventories"
		path "org.zstack.crypto.keyprovider.kms.api.APIQueryKmsReply.inventories"
		desc "KMS密钥提供程序清单"
		type "List"
		since "5.0.0"
		clz KmsInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.kms.api.APIQueryKmsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
