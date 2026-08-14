package org.zstack.accessKey

import org.zstack.header.errorcode.ErrorCode
import org.zstack.accessKey.AccessKeyInventory

doc {

	title "查询 AccessKey 结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.0.0"
	}
	ref {
		name "error"
		path "org.zstack.accessKey.APIQueryAccessKeyReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.0.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.accessKey.APIQueryAccessKeyReply.inventories"
		desc "AccessKey 清单列表"
		type "List"
		since "4.0.0"
		clz AccessKeyInventory.class
	}
}
