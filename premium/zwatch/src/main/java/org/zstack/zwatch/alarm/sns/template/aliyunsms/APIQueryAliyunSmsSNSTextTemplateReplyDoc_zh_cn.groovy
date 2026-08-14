package org.zstack.zwatch.alarm.sns.template.aliyunsms

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.sns.template.aliyunsms.AliyunSmsSNSTextTemplateInventory

doc {

	title "查询SNS阿里云短信文本模板返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.sns.template.aliyunsms.APIQueryAliyunSmsSNSTextTemplateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.zwatch.alarm.sns.template.aliyunsms.APIQueryAliyunSmsSNSTextTemplateReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz AliyunSmsSNSTextTemplateInventory.class
	}
}
