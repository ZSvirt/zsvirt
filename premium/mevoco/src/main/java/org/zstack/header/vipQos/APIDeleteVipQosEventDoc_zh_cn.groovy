package org.zstack.header.vipQos

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除VIPQos"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vipQos.APIDeleteVipQosEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
}
