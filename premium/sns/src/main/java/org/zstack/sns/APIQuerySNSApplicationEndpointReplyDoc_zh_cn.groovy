package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.SNSApplicationEndpointInventory

doc {

	title "查询SNS应用终端返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIQuerySNSApplicationEndpointReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sns.APIQuerySNSApplicationEndpointReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz SNSApplicationEndpointInventory.class
	}
}
