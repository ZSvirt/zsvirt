package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取一台物理机网页终端链接消息回复"

	field {
		name "url"
		desc "物理机网页终端链接"
		type "String"
		since "zsv 3.17.0"
	}
	field {
		name "success"
		desc "是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostWebSshUrlEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
}
