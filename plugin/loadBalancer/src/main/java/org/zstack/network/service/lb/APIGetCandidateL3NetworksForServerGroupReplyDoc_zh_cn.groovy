package org.zstack.network.service.lb

import org.zstack.header.network.l3.L3NetworkInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取服务组对应的候选三层网络的请求返回"

	ref {
		name "inventories"
		path "org.zstack.network.service.lb.APIGetCandidateL3NetworksForServerGroupReply.inventories"
		desc "三层网络清单列表"
		type "List"
		since "3.13.0"
		clz L3NetworkInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.13.0"
	}
	ref {
		name "error"
		path "org.zstack.network.service.lb.APIGetCandidateL3NetworksForServerGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.13.0"
		clz ErrorCode.class
	}
}
