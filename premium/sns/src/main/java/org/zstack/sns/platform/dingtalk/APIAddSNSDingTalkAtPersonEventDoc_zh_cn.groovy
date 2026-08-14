package org.zstack.sns.platform.dingtalk

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.platform.dingtalk.SNSDingTalkAtPersonInventory

doc {

	title "AddSNSDingTalkAtPerson"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.dingtalk.APIAddSNSDingTalkAtPersonEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sns.platform.dingtalk.APIAddSNSDingTalkAtPersonEvent.inventory"
		desc "null"
		type "SNSDingTalkAtPersonInventory"
		since "2.3"
		clz SNSDingTalkAtPersonInventory.class
	}
}
