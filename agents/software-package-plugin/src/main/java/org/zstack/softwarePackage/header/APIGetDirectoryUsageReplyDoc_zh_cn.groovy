package org.zstack.softwarePackage.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取目录容量信息返回"

	field {
		name "totalCapacity"
		desc "总容量"
		type "long"
		since "4.10.20"
	}
	field {
		name "availableCapacity"
		desc "可用容量"
		type "long"
		since "4.10.20"
	}
	field {
		name "success"
		desc "操作成功时为 true，否则为 false"
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.softwarePackage.header.APIGetDirectoryUsageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
