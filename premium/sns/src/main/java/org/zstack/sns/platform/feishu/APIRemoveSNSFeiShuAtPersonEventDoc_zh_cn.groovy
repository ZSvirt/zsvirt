package org.zstack.sns.platform.feishu

import org.zstack.header.errorcode.ErrorCode

doc {

	title "RemoveSNSFeiShuAtPerson"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.feishu.APIRemoveSNSFeiShuAtPersonEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
