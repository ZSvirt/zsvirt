package org.zstack.sns.platform.http

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.platform.http.SNSHttpEndpointInventory

doc {

	title "查询SNS HTTP终端返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.http.APIQuerySNSHttpEndpointReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sns.platform.http.APIQuerySNSHttpEndpointReply.inventories"
		desc "null"
		type "List"
		since "2.3"
		clz SNSHttpEndpointInventory.class
	}
}
