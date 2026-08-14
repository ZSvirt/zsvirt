package org.zstack.resourceconfig

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除资源高级设置的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.resourceconfig.APIDeleteResourceConfigEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4.0"
		clz ErrorCode.class
	}
}
