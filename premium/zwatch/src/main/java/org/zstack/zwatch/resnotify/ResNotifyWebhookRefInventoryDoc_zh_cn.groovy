package org.zstack.zwatch.resnotify



doc {

	title "资源变更通知 webhook 引用清单"

	field {
		name "uuid"
		desc "webhook 引用的UUID，与资源变更通知订阅UUID一致"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "webhookUrl"
		desc "接收资源变更通知的 webhook URL"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "secret"
		desc "用于生成 X-ZStack-Signature 签名的密钥"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "customHeaders"
		desc "投递 webhook 时附加的自定义 HTTP 请求头，JSON 字符串格式"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
}
