package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.SNSWeComAtPersonInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "AddSNSWeComAtPerson"

	ref {
		name "inventory"
		path "org.zstack.sns.platform.wecom.APIAddSNSWeComAtPersonEvent.inventory"
		desc "null"
		type "SNSWeComAtPersonInventory"
		since "zsv 4.2.0"
		clz SNSWeComAtPersonInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.wecom.APIAddSNSWeComAtPersonEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
