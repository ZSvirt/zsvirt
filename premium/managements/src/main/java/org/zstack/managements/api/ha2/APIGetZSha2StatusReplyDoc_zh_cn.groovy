package org.zstack.managements.api.ha2

import org.zstack.managements.entity.ha2.ZSha2StatusView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取管理节点高可用信息的结果"

	ref {
		name "inventory"
		path "org.zstack.managements.api.ha2.APIGetZSha2StatusReply.inventory"
		desc "管理节点信息列表"
		type "ZSha2StatusView"
		since "4.10.20"
		clz ZSha2StatusView.class
	}
	field {
		name "success"
		desc "获取是否成功"
		type "boolean"
		since "4.10.20"
	}
	ref {
		name "error"
		path "org.zstack.managements.api.ha2.APIGetZSha2StatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.10.20"
		clz ErrorCode.class
	}
}
