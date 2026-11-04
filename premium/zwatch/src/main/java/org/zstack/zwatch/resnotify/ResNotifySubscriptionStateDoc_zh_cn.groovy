package org.zstack.zwatch.resnotify



doc {

	title "资源变更通知订阅状态"

	field {
		name "Enabled"
		desc "订阅已启用，会向匹配的通知端点投递事件"
		type "ResNotifySubscriptionState"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "Disabled"
		desc "订阅已禁用，不会投递匹配事件"
		type "ResNotifySubscriptionState"
		since "1.0.0, zsv_5.1.0"
	}
}
