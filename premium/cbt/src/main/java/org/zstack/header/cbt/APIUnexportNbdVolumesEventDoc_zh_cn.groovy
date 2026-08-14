package org.zstack.header.cbt

import org.zstack.header.errorcode.ErrorCode

doc {

	title "停止暴露云盘为 NBD 设备的返回结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.10"
	}
	ref {
		name "error"
		path "org.zstack.header.cbt.APIUnexportNbdVolumesEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.10"
		clz ErrorCode.class
	}
}
