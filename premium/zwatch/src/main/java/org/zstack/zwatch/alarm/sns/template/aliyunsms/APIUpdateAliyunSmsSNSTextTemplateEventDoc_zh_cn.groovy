package org.zstack.zwatch.alarm.sns.template.aliyunsms

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.alarm.sns.template.aliyunsms.AliyunSmsSNSTextTemplateInventory

doc {

	title "更新SNS阿里云短信文本模板返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.sns.template.aliyunsms.APIUpdateAliyunSmsSNSTextTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.alarm.sns.template.aliyunsms.APIUpdateAliyunSmsSNSTextTemplateEvent.inventory"
		desc "null"
		type "AliyunSmsSNSTextTemplateInventory"
		since "0.6"
		clz AliyunSmsSNSTextTemplateInventory.class
	}
}
