package org.zstack.network.hostNetworkInterface.lldp.api

import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "修改 LLDP 的工作模式的请求返回"

	ref {
		name "inventories"
		path "org.zstack.network.hostNetworkInterface.lldp.api.APIChangeHostNetworkInterfaceLldpModeEvent.inventories"
		desc "物理网卡lldp配置清单"
		type "List"
		since "3.11.0"
		clz HostNetworkInterfaceLldpInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.11.0"
	}
	ref {
		name "error"
		path "org.zstack.network.hostNetworkInterface.lldp.api.APIChangeHostNetworkInterfaceLldpModeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.11.0"
		clz ErrorCode.class
	}
}
