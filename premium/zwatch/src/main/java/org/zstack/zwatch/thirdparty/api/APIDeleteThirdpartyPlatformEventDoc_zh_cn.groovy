package org.zstack.zwatch.thirdparty.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除第三方报警源结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.thirdparty.api.APIDeleteThirdpartyPlatformEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10"
		clz ErrorCode.class
	}
}
