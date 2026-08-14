package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.SNSApplicationPlatformInventory

doc {

	title "查询SNS应用平台返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIQuerySNSApplicationPlatformReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sns.APIQuerySNSApplicationPlatformReply.inventories"
		desc "null"
		type "List"
		since "2.3"
		clz SNSApplicationPlatformInventory.class
	}
}
