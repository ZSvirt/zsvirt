package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.SNSSubscriberInventory

doc {

	title "查询sns主题订阅返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIQuerySNSTopicSubscriberReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sns.APIQuerySNSTopicSubscriberReply.inventories"
		desc "null"
		type "List"
		since "2.3.1"
		clz SNSSubscriberInventory.class
	}
}
