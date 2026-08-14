package org.zstack.header.network.l3

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除IP地址"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 4.10.16"
	}
	ref {
		name "error"
		path "org.zstack.header.network.l3.APIDeleteIpAddressEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 4.10.16"
		clz ErrorCode.class
	}
}
