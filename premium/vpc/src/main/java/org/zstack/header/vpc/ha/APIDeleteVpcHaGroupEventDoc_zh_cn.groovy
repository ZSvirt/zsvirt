package org.zstack.header.vpc.ha

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除高可用组结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vpc.ha.APIDeleteVpcHaGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5"
		clz ErrorCode.class
	}
}
