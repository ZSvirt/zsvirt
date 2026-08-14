package org.zstack.sns.platform.email

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除邮箱接收端的地址的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.email.APIDeleteEmailAddressOfSNSEmailEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
}
