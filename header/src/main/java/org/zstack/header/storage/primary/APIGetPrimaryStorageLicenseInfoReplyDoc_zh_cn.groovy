package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "主存储License信息"

	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIGetPrimaryStorageLicenseInfoReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.6.0"
		clz ErrorCode.class
	}
	field {
		name "uuid"
		desc ""
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "name"
		desc ""
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "expireTime"
		desc ""
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 3.6.0"
	}
}
