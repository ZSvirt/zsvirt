package org.zstack.header.host

import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机交集绑定结果"

	ref {
		name "inventories"
		path "org.zstack.header.host.APIGetCandidateNetworkBondingsReply.inventories"
		desc "网络绑定清单列表"
		type "List"
		since "3.18.0"
		clz HostNetworkBondingInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.18.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetCandidateNetworkBondingsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.18.0"
		clz ErrorCode.class
	}
}
