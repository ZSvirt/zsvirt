package org.zstack.zwatch.resnotify



doc {

	title "资源变更通知订阅类型"

	field {
		name "WEBHOOK"
		desc "通过 HTTP webhook 投递资源变更通知"
		type "ResNotifyType"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "WEBSOCKET"
		desc "通过 WebSocket 投递资源变更通知，当前预留"
		type "ResNotifyType"
		since "1.0.0, zsv_5.1.0"
	}
}
