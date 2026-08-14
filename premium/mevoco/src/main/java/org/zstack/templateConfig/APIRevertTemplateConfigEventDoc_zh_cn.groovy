package org.zstack.templateConfig

import org.zstack.header.errorcode.ErrorCode

doc {

	title "重置模板配置到默认值返回信息"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.templateConfig.APIRevertTemplateConfigEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
