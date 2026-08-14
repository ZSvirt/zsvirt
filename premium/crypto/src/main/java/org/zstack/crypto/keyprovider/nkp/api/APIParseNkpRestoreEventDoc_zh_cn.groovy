package org.zstack.crypto.keyprovider.nkp.api

import org.zstack.header.keyprovider.NkpRestoreInfo
import org.zstack.header.errorcode.ErrorCode

doc {

	title "解析原生密钥提供程序导入结果"

	ref {
		name "restoreInfo"
		path "org.zstack.crypto.keyprovider.nkp.api.APIParseNkpRestoreEvent.restoreInfo"
		desc "null"
		type "NkpRestoreInfo"
		since "5.0.0"
		clz NkpRestoreInfo.class
	}
	field {
		name "code"
		desc "解析结果码"
		type "String"
		since "5.0.0"
	}
	field {
		name "reason"
		desc "解析失败原因"
		type "String"
		since "5.0.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.nkp.api.APIParseNkpRestoreEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
