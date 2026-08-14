package org.zstack.managements.api.common

import org.zstack.managements.entity.common.ManagementsStatusView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取管理节点信息的结果"

	ref {
		name "inventory"
		path "org.zstack.managements.api.common.APIGetManagementNodesStatusReply.inventory"
		desc "管理节点信息列表"
		type "ManagementsStatusView"
		since "4.10.20"
		clz ManagementsStatusView.class
	}
	field {
		name "success"
		desc "获取是否成功"
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.managements.api.common.APIGetManagementNodesStatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
