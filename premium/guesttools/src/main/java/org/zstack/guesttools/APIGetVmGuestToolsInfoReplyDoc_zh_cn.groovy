package org.zstack.guesttools

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取云主机内部增强工具信息的结果"

	ref {
		name "error"
		path "org.zstack.guesttools.APIGetVmGuestToolsInfoReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.7.0"
		clz ErrorCode.class
	}
	field {
		name "version"
		desc "增强工具版本"
		type "String"
		since "3.7.0"
	}
	field {
		name "status"
		desc "增强工具运行状态"
		type "String"
		since "3.7.0"
	}
	field {
		name "features"
		desc "增强工具支持的功能, 以及功能模块的相关状态"
		type "Map"
		since "3.12.0"
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "3.7.0"
	}
}
