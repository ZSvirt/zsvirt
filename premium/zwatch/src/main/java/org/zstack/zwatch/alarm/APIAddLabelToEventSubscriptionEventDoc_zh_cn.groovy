package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.EventSubscriptionLabelInventory

doc {

	title "添加标签到事件订阅返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIAddLabelToEventSubscriptionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APIAddLabelToEventSubscriptionEvent.inventory"
		desc "事件结构"
		type "EventSubscriptionLabelInventory"
		since "2.3.1"
		clz EventSubscriptionLabelInventory.class
	}
}
