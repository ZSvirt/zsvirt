package org.zstack.sns.platform.snmp

import org.zstack.header.errorcode.ErrorCode

doc {

	title "SNMP发送测试消息响应"

	field {
		name "success"
		desc ""
		type "boolean"
		since "4.10.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.snmp.APISNSSnmpTestConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.0"
		clz ErrorCode.class
	}
	field {
		name "connected"
		desc "是否联通"
		type "boolean"
		since "4.10.0"
	}
	field {
		name "webhookResp"
		desc "对端响应"
		type "LinkedHashMap"
		since "4.10.0"
	}
}