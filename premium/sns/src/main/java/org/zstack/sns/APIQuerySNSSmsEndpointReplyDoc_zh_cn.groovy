package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.SNSSmsEndpointInventory

doc {

	title "查询短信接收端返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIQuerySNSSmsEndpointReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sns.APIQuerySNSSmsEndpointReply.inventories"
		desc "null"
		type "List"
		since "3.7.0"
		clz SNSSmsEndpointInventory.class
	}
}
