package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.SNSFeiShuEndpointInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "QuerySNSFeiShuEndpoint"

	ref {
		name "inventories"
		path "org.zstack.sns.platform.feishu.APIQuerySNSFeiShuEndpointReply.inventories"
		desc "查询SNS飞书终端结果列表"
		type "List"
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
		path "org.zstack.sns.platform.feishu.APIQuerySNSFeiShuEndpointReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
