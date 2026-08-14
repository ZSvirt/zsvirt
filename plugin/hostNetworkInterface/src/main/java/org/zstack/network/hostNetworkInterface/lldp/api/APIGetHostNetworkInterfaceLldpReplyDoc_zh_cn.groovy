package org.zstack.network.hostNetworkInterface.lldp.api

import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpRefInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理网口lldp信息"

	ref {
		name "lldp"
		path "org.zstack.network.hostNetworkInterface.lldp.api.APIGetHostNetworkInterfaceLldpReply.lldp"
		desc "物理网卡lldp信息清单"
		type "HostNetworkInterfaceLldpRefInventory"
		since "4.1.0"
		clz HostNetworkInterfaceLldpRefInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "4.1.0"
	}
	ref {
		name "error"
		path "org.zstack.network.hostNetworkInterface.lldp.api.APIGetHostNetworkInterfaceLldpReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "4.1.0"
		clz ErrorCode.class
	}
}
