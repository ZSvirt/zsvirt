package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.EventSubscriptionInventory

doc {

	title "事件订阅返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APISubscribeEventEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APISubscribeEventEvent.inventory"
		desc "null"
		type "EventSubscriptionInventory"
		since "2.3"
		clz EventSubscriptionInventory.class
	}
}
