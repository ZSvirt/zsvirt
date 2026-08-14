package org.zstack.sns

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.SNSApplicationEndpointInventory

doc {

	title "更新SNS应用终端返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.APIUpdateSNSApplicationEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sns.APIUpdateSNSApplicationEndpointEvent.inventory"
		desc "null"
		type "SNSApplicationEndpointInventory"
		since "2.3"
		clz SNSApplicationEndpointInventory.class
	}
}
