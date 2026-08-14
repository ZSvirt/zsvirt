package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.SNSWeComAtPersonInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "QuerySNSWeComAtPerson"

	ref {
		name "inventories"
		path "org.zstack.sns.platform.wecom.APIQuerySNSWeComAtPersonReply.inventories"
		desc "查询SNS企业微信@用户结果列表"
		type "List"
		since "zsv 4.2.0"
		clz SNSWeComAtPersonInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.wecom.APIQuerySNSWeComAtPersonReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
