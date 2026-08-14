package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "管理节点当前版本"

	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetVersionReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "version"
		desc "管理节点当前版本"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "success"
		desc "成功标志"
		type "boolean"
		since "zsv 0.6"
	}
}
