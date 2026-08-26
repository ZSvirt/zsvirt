package org.zstack.zwatch.resnotify

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除资源变更通知订阅事件"

	field {
		name "success"
		desc "删除资源变更通知订阅是否成功"
		type "boolean"
		since "1.0.1, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.resnotify.APIDeleteResNotifySubscriptionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.0.1, zsv_5.1.0"
		clz ErrorCode.class
	}
}
