package org.zstack.sns.platform.http

import org.zstack.header.errorcode.ErrorCode
import java.lang.Object

doc {

	title "发送测试HTTP请求响应"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.http.APISNSHttpTestConnectionEvent.error"
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
		desc "对端响应结果"
		type "Object"
		since "4.10.0"
	}
}