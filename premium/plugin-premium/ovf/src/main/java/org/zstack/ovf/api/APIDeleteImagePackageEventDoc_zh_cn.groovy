package org.zstack.ovf.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除镜像包返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.14.6"
	}
	ref {
		name "error"
		path "org.zstack.ovf.api.APIDeleteImagePackageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.14.6"
		clz ErrorCode.class
	}
}
