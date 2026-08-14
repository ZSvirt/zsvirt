package org.zstack.header.host


import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机物理网络信息清单"

	ref {
		name "error"
		path "org.zstack.header.host.APIGetHostNetworkFactsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5.0"
		clz ErrorCode.class
	}
	ref {
		name "bondings"
		path "org.zstack.header.host.APIGetHostNetworkFactsReply.bondings"
		desc "bond设备"
		type "List"
		since "3.5.0"
		clz HostNetworkBondingInventory.class
	}
	ref {
		name "nics"
		path "org.zstack.header.host.APIGetHostNetworkFactsReply.nics"
		desc "网卡设备"
		type "List"
		since "3.5.0"
		clz HostNetworkInterfaceInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.5.0"
	}
}
