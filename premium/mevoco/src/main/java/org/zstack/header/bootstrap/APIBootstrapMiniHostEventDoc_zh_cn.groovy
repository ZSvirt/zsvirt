package org.zstack.header.bootstrap

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "初始化Mini一体机结果"

	ref {
		name "error"
		path "org.zstack.header.bootstrap.APIBootstrapMiniHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6.0"
		clz ErrorCode.class
	}
	field {
		name "stage"
		desc "失败阶段"
		type "String"
		since "3.6.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.6.0"
	}
}
