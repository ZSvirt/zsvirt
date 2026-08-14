package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除短信接收者返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIRemoveSNSSmsReceiverEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
}
