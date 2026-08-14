package org.zstack.header.host

import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机交集网卡信息"

	field {
		name "slaveNames"
		desc "网卡名称"
		type "List"
		since "3.17.0"
	}
	ref {
		name "candidateNics"
		path "org.zstack.header.host.APIGetCandidateNetworkInterfacesReply.candidateNics"
		desc "候选网卡列表"
		type "List"
		since "3.17.0"
		clz HostNetworkInterfaceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetCandidateNetworkInterfacesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
