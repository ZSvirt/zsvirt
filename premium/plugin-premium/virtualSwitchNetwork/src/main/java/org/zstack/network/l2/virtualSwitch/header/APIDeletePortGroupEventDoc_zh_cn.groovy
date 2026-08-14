package org.zstack.network.l2.virtualSwitch.header

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除端口组结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.2.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APIDeletePortGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.2.0"
		clz ErrorCode.class
	}
}
