package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.SNSSmsReceiverInventory

doc {

	title "添加短信接收者返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIAddSNSSmsReceiverEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sns.APIAddSNSSmsReceiverEvent.inventory"
		desc "null"
		type "SNSSmsReceiverInventory"
		since "3.7.0"
		clz SNSSmsReceiverInventory.class
	}
}
