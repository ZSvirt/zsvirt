package org.zstack.nas

import org.zstack.header.errorcode.ErrorCode
import org.zstack.nas.NasFileSystemInventory

doc {

	title "更新后的NAS文件系统清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.nas.APIUpdateNasFileSystemEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.nas.APIUpdateNasFileSystemEvent.inventory"
		desc "更新后的NAS文件系统清单"
		type "NasFileSystemInventory"
		since "2.4.0"
		clz NasFileSystemInventory.class
	}
}
