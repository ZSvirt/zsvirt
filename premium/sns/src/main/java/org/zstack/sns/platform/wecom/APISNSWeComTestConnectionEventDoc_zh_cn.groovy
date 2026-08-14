package org.zstack.sns.platform.wecom

import org.zstack.header.errorcode.ErrorCode

doc {

	title "SNSWeComTestConnection"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.wecom.APISNSWeComTestConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
	field {
		name "connected"
		desc "测试Webhook URL是否联通"
		type "boolean"
		since "zsv 4.2.0"
	}
	field {
		name "webhookResp"
		desc "Webhook返回数据"
		type "boolean"
		since "zsv 4.2.0"
	}
}
