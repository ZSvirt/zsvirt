package org.zstack.header.protocol

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.protocol.Neighbor

doc {

	title "在这里输入结构的名称"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.protocol.APIGetVRouterOspfNeighborReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.4"
		clz ErrorCode.class
	}
	ref {
		name "neighbors"
		path "org.zstack.header.protocol.APIGetVRouterOspfNeighborReply.neighbors"
		desc "null"
		type "List"
		since "zsv_3.4"
		clz Neighbor.class
	}
}
