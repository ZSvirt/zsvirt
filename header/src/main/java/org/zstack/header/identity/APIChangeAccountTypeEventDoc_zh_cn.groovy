package org.zstack.header.identity

import org.zstack.header.identity.APIChangeAccountTypeEvent

doc {
	title "ChangeAccountType"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 5.0.0"
	}

	ref {
		name "error"
		path "org.zstack.header.identity.APIChangeAccountTypeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
