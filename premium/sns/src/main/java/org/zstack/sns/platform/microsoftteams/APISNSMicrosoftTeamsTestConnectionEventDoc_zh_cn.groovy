package org.zstack.sns.platform.microsoftteams

import org.zstack.header.errorcode.ErrorCode

doc {

	title "Microsoft Teams 测试连通性(发送测试消息)"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.microsoftteams.APISNSMicrosoftTeamsTestConnectionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.2.0"
		clz ErrorCode.class
	}
	field {
		name "connected"
		desc "Webhook URL是否联通"
		type "boolean"
		since "4.2.0"
	}
	field {
		name "webhookResp"
		desc "Webhook返回数据"
		type "LinkedHashMap"
		since "4.2.0"
	}
}
