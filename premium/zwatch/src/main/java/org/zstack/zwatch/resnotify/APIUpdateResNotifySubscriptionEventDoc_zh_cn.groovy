package org.zstack.zwatch.resnotify

import org.zstack.zwatch.resnotify.ResNotifySubscriptionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新资源变更通知订阅事件"

	ref {
		name "inventory"
		path "org.zstack.zwatch.resnotify.APIUpdateResNotifySubscriptionEvent.inventory"
		desc "更新后的资源变更通知订阅清单"
		type "ResNotifySubscriptionInventory"
		since "1.0.0, zsv_5.1.0"
		clz ResNotifySubscriptionInventory.class
	}
	field {
		name "success"
		desc "更新资源变更通知订阅是否成功"
		type "boolean"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.resnotify.APIUpdateResNotifySubscriptionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.0.0, zsv_5.1.0"
		clz ErrorCode.class
	}
}
