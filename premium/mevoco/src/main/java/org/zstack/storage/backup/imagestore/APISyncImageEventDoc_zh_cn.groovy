package org.zstack.storage.backup.imagestore

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从镜像服务器元数据中同步镜像返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.storage.backup.imagestore.APISyncImageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
