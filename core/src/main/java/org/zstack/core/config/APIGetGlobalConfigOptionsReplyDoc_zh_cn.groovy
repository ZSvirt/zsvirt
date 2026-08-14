package org.zstack.core.config

import org.zstack.header.errorcode.ErrorCode
import org.zstack.core.config.GlobalConfigOptions

doc {

	title "获取全局配置可用值"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.14.0"
	}
	ref {
		name "error"
		path "org.zstack.core.config.APIGetGlobalConfigOptionsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.14.0"
		clz ErrorCode.class
	}
	ref {
		name "options"
		path "org.zstack.core.config.APIGetGlobalConfigOptionsReply.options"
		desc "全局配置可用值"
		type "GlobalConfigOptions"
		since "3.14.0"
		clz GlobalConfigOptions.class
	}
}
