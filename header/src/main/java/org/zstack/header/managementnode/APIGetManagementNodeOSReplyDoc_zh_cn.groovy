package org.zstack.header.managementnode

import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取管理节点系统的返回"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 3.11.2"
	}
	field {
		name "version"
		desc "管理节点版本"
		type "String"
		since "zsv 3.11.2"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.11.2"
	}
	ref {
		name "error"
		path "org.zstack.header.managementnode.APIGetManagementNodeOSReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.11.2"
		clz ErrorCode.class
	}
}
