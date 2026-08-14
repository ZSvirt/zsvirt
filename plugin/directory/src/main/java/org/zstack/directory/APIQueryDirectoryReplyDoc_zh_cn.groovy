package org.zstack.directory

import org.zstack.directory.DirectoryInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询目录分组的返回"

	ref {
		name "inventories"
		path "org.zstack.directory.APIQueryDirectoryReply.inventories"
		desc "目录分组清单列表"
		type "List"
		since "3.17.0"
		clz DirectoryInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.directory.APIQueryDirectoryReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
