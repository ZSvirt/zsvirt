package org.zstack.zsv.core.api

import org.zstack.zsv.core.entity.NodeRolesView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "判断管理节点是否有其它的角色的结果"

	ref {
		name "inventories"
		path "org.zstack.zsv.core.api.APIIsManagementAlsoComputeReply.inventories"
		desc "判断结果数据"
		type "List"
		since "4.10.7"
		clz NodeRolesView.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.10.7"
	}
	ref {
		name "error"
		path "org.zstack.zsv.core.api.APIIsManagementAlsoComputeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.10.7"
		clz ErrorCode.class
	}
}
