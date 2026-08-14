package org.zstack.header.core.encrypt

import org.zstack.header.errorcode.ErrorCode

doc {

	title "加密字段返回"

	field {
		name "encryptedFields"
		desc "所有加密字段列表，列表中每个项是jsonString格式"
		type "List"
		since "zsv 3.17.0"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.encrypt.APIGetEncryptedFieldReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
