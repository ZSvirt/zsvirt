package org.zstack.sns.platform.email

import org.zstack.header.errorcode.ErrorCode

doc {

	title "发送测试邮件响应"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.email.APISNSEmailTestConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
	field {
		name "connected"
		desc "发送结果"
		type "boolean"
		since "4.10.0"
	}
	field {
		name "webhookResp"
		desc "对端响应内容"
		type "LinkedHashMap"
		since "4.10.0"
	}
}