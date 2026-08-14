package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从事件订阅里删除动作"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIRemoveActionFromEventSubscriptionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3.1"
		clz ErrorCode.class
	}
}
