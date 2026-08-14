package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.EventSubscriptionInventory

doc {

	title "修改事件报警器返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIUpdateSubscribeEventEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.8"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APIUpdateSubscribeEventEvent.inventory"
		desc "null"
		type "EventSubscriptionInventory"
		since "3.8"
		clz EventSubscriptionInventory.class
	}
}
