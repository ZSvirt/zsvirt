package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode

doc {

	title "设置镜像启动模式的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APISetImageBootModeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.9.0"
		clz ErrorCode.class
	}
}
