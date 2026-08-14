package org.zstack.sns.platform.dingtalk

import org.zstack.sns.platform.dingtalk.SNSDingTalkAtPersonInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "UpdateAtPersonOfAtDingTalkEndpoint"

	ref {
		name "inventory"
		path "org.zstack.sns.platform.dingtalk.APIUpdateAtPersonOfDingTalkEndpointEvent.inventory"
		desc "更新钉钉@用户结果"
		type "SNSDingTalkAtPersonInventory"
		since "zsv 4.2.0"
		clz SNSDingTalkAtPersonInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.dingtalk.APIUpdateAtPersonOfDingTalkEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
