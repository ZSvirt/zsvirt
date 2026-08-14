package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "添加存储协议event"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIAddStorageProtocolEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 5.1.0"
		clz ErrorCode.class
	}
}
