package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.EventSubscriptionLabelInventory

doc {

	title "更新事件订阅的标签结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIUpdateEventSubscriptionLabelEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APIUpdateEventSubscriptionLabelEvent.inventory"
		desc "事件订阅标签"
		type "EventSubscriptionLabelInventory"
		since "3.9.0"
		clz EventSubscriptionLabelInventory.class
	}
}
