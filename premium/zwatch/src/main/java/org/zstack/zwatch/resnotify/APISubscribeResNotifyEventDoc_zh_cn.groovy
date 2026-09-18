package org.zstack.zwatch.resnotify

import org.zstack.zwatch.resnotify.ResNotifySubscriptionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "订阅资源变更通知事件"

	ref {
		name "inventory"
		path "org.zstack.zwatch.resnotify.APISubscribeResNotifyEvent.inventory"
		desc "已创建的资源变更通知订阅清单"
		type "ResNotifySubscriptionInventory"
		since "1.0.0, zsv_5.1.0"
		clz ResNotifySubscriptionInventory.class
	}
	field {
		name "success"
		desc "订阅资源变更通知是否成功"
		type "boolean"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.resnotify.APISubscribeResNotifyEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.0.0, zsv_5.1.0"
		clz ErrorCode.class
	}
}
