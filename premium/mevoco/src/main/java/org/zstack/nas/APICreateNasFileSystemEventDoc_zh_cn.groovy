package org.zstack.nas

import org.zstack.header.errorcode.ErrorCode
import org.zstack.nas.NasFileSystemInventory

doc {

	title "阿里云NAS文件系统清单列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.nas.APICreateNasFileSystemEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.nas.APICreateNasFileSystemEvent.inventory"
		desc "阿里云NAS文件系统清单"
		type "NasFileSystemInventory"
		since "2.4.0"
		clz NasFileSystemInventory.class
	}
}
