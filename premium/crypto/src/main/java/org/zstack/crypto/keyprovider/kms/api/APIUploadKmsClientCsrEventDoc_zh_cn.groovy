package org.zstack.crypto.keyprovider.kms.api

import org.zstack.header.keyprovider.KmsIdentityInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "上传KMS客户端CSR与私钥结果"

	ref {
		name "inventory"
		path "org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientCsrEvent.inventory"
		desc "null"
		type "KmsIdentityInventory"
		since "5.0.0"
		clz KmsIdentityInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.kms.api.APIUploadKmsClientCsrEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
