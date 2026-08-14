package org.zstack.templateConfig

import org.zstack.header.errorcode.ErrorCode

doc {

	title "重置模板返回信息"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.templateConfig.APIResetTemplateConfigEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6.0"
		clz ErrorCode.class
	}
}
