package org.zstack.directory

import org.zstack.directory.DirectoryInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建目录返回"

	ref {
		name "inventory"
		path "org.zstack.directory.APICreateDirectoryEvent.inventory"
		desc "目录清单"
		type "DirectoryInventory"
		since "3.16.0"
		clz DirectoryInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.0"
	}
	ref {
		name "error"
		path "org.zstack.directory.APICreateDirectoryEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.0"
		clz ErrorCode.class
	}
}
