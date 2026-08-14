package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取管理节点时区信息结果"

	ref {
		name "error"
		path "org.zstaheader.managementnode.APIGetPlatformTimeZoneReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.11.0"
		clz ErrorCode.class
	}
	field {
		name "timezone"
		desc "时区名称"
		type "String"
		since "zsv 3.11.0"
	}
	field {
		name "offset"
		desc "与GMT时间的时差"
		type "String"
		since "zsv 3.11.0"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.11.0"
	}
}
