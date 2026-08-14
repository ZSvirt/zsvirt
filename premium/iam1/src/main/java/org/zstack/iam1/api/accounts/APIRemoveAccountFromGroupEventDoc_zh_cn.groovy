package org.zstack.iam1.api.accounts

import org.zstack.header.errorcode.ErrorCode

doc {

	title "账户组移除账户结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.iam1.api.accounts.APIRemoveAccountFromGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
}
