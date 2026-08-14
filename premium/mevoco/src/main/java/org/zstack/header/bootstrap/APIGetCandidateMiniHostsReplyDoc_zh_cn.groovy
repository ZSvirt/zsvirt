package org.zstack.header.bootstrap

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.bootstrap.MiniCandidateHostStruct

doc {

	title "获取未添加的Mini物理机清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.bootstrap.APIGetCandidateMiniHostsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "hosts"
		path "org.zstack.header.bootstrap.APIGetCandidateMiniHostsReply.hosts"
		desc "null"
		type "List"
		since "3.6.0"
		clz MiniCandidateHostStruct.class
	}
}
