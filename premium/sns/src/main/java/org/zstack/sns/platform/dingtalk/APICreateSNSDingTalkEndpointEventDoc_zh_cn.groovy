package org.zstack.sns.platform.dingtalk

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.platform.dingtalk.SNSDingTalkEndpointInventory

doc {

	title "CreateSNSDingTalkEndpoint"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.dingtalk.APICreateSNSDingTalkEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sns.platform.dingtalk.APICreateSNSDingTalkEndpointEvent.inventory"
		desc "null"
		type "SNSDingTalkEndpointInventory"
		since "2.3"
		clz SNSDingTalkEndpointInventory.class
	}
}
