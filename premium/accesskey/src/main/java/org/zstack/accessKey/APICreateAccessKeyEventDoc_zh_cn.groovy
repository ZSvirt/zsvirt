package org.zstack.accessKey

import org.zstack.header.errorcode.ErrorCode
import org.zstack.accessKey.AccessKeyInventory

doc {

	title "创建 AccessKey 结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.accessKey.APICreateAccessKeyEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.accessKey.APICreateAccessKeyEvent.inventory"
		desc "AccessKey 清单"
		type "AccessKeyInventory"
		since "4.0.0"
		clz AccessKeyInventory.class
	}
}
