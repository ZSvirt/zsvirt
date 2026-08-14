package org.zstack.sns.platform.email

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.platform.email.SNSEmailAddressInventory

doc {

	title "更新接收端邮箱地址的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.email.APIUpdateEmailAddressOfSNSEmailEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sns.platform.email.APIUpdateEmailAddressOfSNSEmailEndpointEvent.inventory"
		desc "null"
		type "SNSEmailAddressInventory"
		since "3.7.0"
		clz SNSEmailAddressInventory.class
	}
}
