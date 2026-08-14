package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.SNSWeComEndpointInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "QuerySNSWeComEndpoint"

	ref {
		name "inventories"
		path "org.zstack.sns.platform.wecom.APIQuerySNSWeComEndpointReply.inventories"
		desc "查询SNS企业微信终端结果列表"
		type "List"
		since "zsv 4.2.0"
		clz SNSWeComEndpointInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.wecom.APIQuerySNSWeComEndpointReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
