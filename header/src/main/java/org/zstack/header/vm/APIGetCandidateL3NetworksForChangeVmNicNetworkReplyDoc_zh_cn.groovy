package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l3.L3NetworkInventory

doc {

	title "获取虚拟机网卡可加载网络的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetCandidateL3NetworksForChangeVmNicNetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.11.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.APIGetCandidateL3NetworksForChangeVmNicNetworkReply.inventories"
		desc "虚拟机网卡可加载网络清单列表"
		type "List"
		since "zsv 3.11.0"
		clz L3NetworkInventory.class
	}
}
