package org.zstack.header.storage.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取镜像服务器存储容量"

	ref {
		name "error"
		path "org.zstack.header.storage.backup.APIGetBackupStorageCapacityReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "totalCapacity"
		desc "总容量（以字节计）"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "availableCapacity"
		desc "可用容量（以字节计）"
		type "long"
		since "zsv 0.6"
	}
	field {
		name "success"
		desc "成功失败标志"
		type "boolean"
		since "zsv 0.6"
	}
}
