package org.zstack.sns.platform.feishu

import org.zstack.header.errorcode.ErrorCode

doc {

	title "SNSFeiShuTestConnection"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.feishu.APISNSFeiShuTestConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
	field {
		name "connected"
		desc "表示Webhook URL是否联通"
		type "boolean"
		since "zsv 4.2.0"
	}
	field {
		name "webhookResp"
		desc "飞书返回消息 (示例: https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot#e2f7069c)"
		type "webhookResp"
		since "zsv 4.2.0"
	}
}
