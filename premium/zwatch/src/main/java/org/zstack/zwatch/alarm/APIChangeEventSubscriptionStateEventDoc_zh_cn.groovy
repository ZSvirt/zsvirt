package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.EventSubscriptionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改事件报警器状态返回"

	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.APIChangeEventSubscriptionStateEvent.inventory"
		desc "事件订阅清单"
		type "EventSubscriptionInventory"
		since "3.17.0"
		clz EventSubscriptionInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIChangeEventSubscriptionStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
