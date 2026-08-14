package org.zstack.zwatch.alarm.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory

doc {

	title "查询报警器消息模版的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "2.3"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.sns.APIQuerySNSTextTemplateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.zwatch.alarm.sns.APIQuerySNSTextTemplateReply.inventories"
		desc "报警器消息模版清单"
		type "List"
		since "2.3"
		clz SNSTextTemplateInventory.class
	}
}
