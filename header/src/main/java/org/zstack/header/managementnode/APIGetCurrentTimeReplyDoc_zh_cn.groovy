package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "管理节点当前时间"

	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetCurrentTimeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "currentTime"
		desc "管理节点当前时间"
		type "Map"
		since "zsv 0.6"
	}
	field {
		name "success"
		desc "成功标志"
		type "boolean"
		since "zsv 0.6"
	}
}
