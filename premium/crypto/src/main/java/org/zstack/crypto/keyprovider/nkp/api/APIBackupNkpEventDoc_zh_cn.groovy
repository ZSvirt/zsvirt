package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "备份原生密钥提供程序结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.nkp.api.APIBackupNkpEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
	field {
		name "content"
		desc "备份内容（Base64编码）"
		type "String"
		since "5.0.0"
	}
}
