package org.zstack.sns.platform.dingtalk

import org.zstack.header.errorcode.ErrorCode

doc {

	title "SNSDingTalkTestConnection"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.dingtalk.APISNSDingTalkTestConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
	field {
		name "connected"
		desc "Webhook URL连通性标识(仅代表能否联通)"
		type "boolean"
		since "zsv 4.2.0"
	}
	field {
		name "webhookResp"
		desc "Webhook URL发送测试消息对端返回 (示例: https://open.dingtalk.com/document/robots/custom-robot-access#title-7ur-3ok-s1a)"
		type "LinkedHashMap"
		since "zsv 4.2.0"
	}
}
