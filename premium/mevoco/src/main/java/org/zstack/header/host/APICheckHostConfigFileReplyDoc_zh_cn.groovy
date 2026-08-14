package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "检查添加物理机文件合法性"

	ref {
		name "error"
		path "org.zstack.header.host.APICheckHostConfigFileReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.1.0"
	}
}
