package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取内部依赖的L3网络或者镜像服务器信息"

	field {
		name "inventories"
		desc ""
		type "List"
		since "zsv 0.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetInterdependentL3NetworksBackupStoragesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
}
