package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.SNSFeiShuAtPersonInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "UpdateAtPersonOfAtFeiShuEndpoint"

	ref {
		name "inventory"
		path "org.zstack.sns.platform.feishu.APIUpdateAtPersonOfFeiShuEndpointEvent.inventory"
		desc "更新飞书@用户结果"
		type "SNSFeiShuAtPersonInventory"
		since "zsv 4.2.0"
		clz SNSFeiShuAtPersonInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.feishu.APIUpdateAtPersonOfFeiShuEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
