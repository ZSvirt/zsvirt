package org.zstack.zwatch.thirdparty.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改第三方报警消息结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.thirdparty.api.APIUpdateThirdpartyAlertsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10"
		clz ErrorCode.class
	}
}
