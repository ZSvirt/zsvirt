package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory

doc {

	title "查询物理机网卡信息的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIQueryHostNetworkInterfaceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.host.APIQueryHostNetworkInterfaceReply.inventories"
		desc "物理机网卡设备清单"
		type "List"
		since "3.9.0"
		clz HostNetworkInterfaceInventory.class
	}
}
