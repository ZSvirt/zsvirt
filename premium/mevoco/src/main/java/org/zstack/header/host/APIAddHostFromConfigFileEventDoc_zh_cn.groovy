package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode

doc {

	title "通过文件添加物理机"

	ref {
		name "error"
		path "org.zstack.header.host.APIAddHostFromConfigFileEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "results"
		path "org.zstack.header.host.APIAddHostFromConfigFileEvent.results"
		desc "添加物理机的结果"
		type "List"
		since "3.1.0"
		clz AddHostFromFileResult.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.1.0"
	}
}
