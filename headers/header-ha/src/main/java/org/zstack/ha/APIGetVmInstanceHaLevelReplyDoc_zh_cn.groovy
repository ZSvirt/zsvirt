package org.zstack.ha

import org.zstack.header.errorcode.ErrorCode

doc {

	title "云主机高可用级别"

	ref {
		name "error"
		path "org.zstack.ha.APIGetVmInstanceHaLevelReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "level"
		desc "云主机高可用级别，NeverStop或OnHostFailure"
		type "String"
		since "0.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
}
