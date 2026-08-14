package org.zstack.zwatch.alarm.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.sns.SNSTextTemplateInventory

doc {

	title "更新报警器消息模版的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "2.3"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.sns.APIUpdateSNSTextTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.sns.APIUpdateSNSTextTemplateEvent.inventory"
		desc "报警器消息模版清单"
		type "SNSTextTemplateInventory"
		since "2.3"
		clz SNSTextTemplateInventory.class
	}
}
