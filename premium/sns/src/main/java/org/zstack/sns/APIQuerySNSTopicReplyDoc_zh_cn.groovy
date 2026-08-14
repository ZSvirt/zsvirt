package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.SNSTopicInventory

doc {

	title "查询SNS应用主题返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIQuerySNSTopicReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sns.APIQuerySNSTopicReply.inventories"
		desc "null"
		type "List"
		since "2.3"
		clz SNSTopicInventory.class
	}
}
