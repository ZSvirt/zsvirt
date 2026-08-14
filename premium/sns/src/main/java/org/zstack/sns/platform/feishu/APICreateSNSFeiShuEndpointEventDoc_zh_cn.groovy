package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.SNSFeiShuEndpointInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "CreateSNSFeiShuEndpoint"

	ref {
		name "inventory"
		path "org.zstack.sns.platform.feishu.APICreateSNSFeiShuEndpointEvent.inventory"
		desc "null"
		type "SNSFeiShuEndpointInventory"
		since "zsv 4.2.0"
		clz SNSFeiShuEndpointInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.feishu.APICreateSNSFeiShuEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
