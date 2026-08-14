package org.zstack.header.tpm.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "虚拟机删除 TPM 的结果"

	field {
		name "success"
		desc "删除是否成功"
		type "boolean"
		since "zsv 5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.tpm.api.APIRemoveTpmEvent.error"
		desc "错误码，若不为 null，则表示操作失败, 操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
