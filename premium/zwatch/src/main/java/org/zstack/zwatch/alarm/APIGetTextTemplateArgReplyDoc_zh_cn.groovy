package org.zstack.zwatch.alarm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询模板默认支持参数列表"

	field {
		name "defaultSupportedParams"
		desc "默认支持参数列表"
		type "Map"
		since "zsv 4.2.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.alarm.APIGetTextTemplateArgReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.2.0"
		clz ErrorCode.class
	}
}
